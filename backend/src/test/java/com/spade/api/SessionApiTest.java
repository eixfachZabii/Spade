package com.spade.api;

import static com.spade.api.TestPlayers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import com.spade.service.Hashing;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpadeIntegrationTest
class SessionApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    TestPlayers players;

    @BeforeEach
    void setUp() {
        players = new TestPlayers(mvc);
    }

    @Test
    void logsInOnceAndTheTokenOpensMe() throws Exception {
        String name = players.register(TestPlayers.unique("ana"));
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"correct-horse","deviceName":"Ana's iPhone"}""".formatted(name.toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.device.deviceName").value("Ana's iPhone"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        assertThat(token).hasSize(43);   // 32 random bytes, base64url without padding

        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));
    }

    @Test
    void storesOnlyTheTokensHash() throws Exception {
        String name = players.register(TestPlayers.unique("ben"));
        String token = players.login(name);
        List<String> stored = jdbc.queryForList(
                "select s.token_sha256 from device_session s join player p on p.id = s.player_id where p.username = ?",
                String.class, name);
        assertThat(stored).containsExactly(Hashing.sha256Hex(token));
    }

    @Test
    void aWrongPasswordAndAnUnknownUserLookTheSame() throws Exception {
        String name = players.register(TestPlayers.unique("cleo"));
        String wrongPassword = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"nope-nope","deviceName":"x"}""".formatted(name)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownUser = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"nope-nope","deviceName":"x"}""".formatted(TestPlayers.unique("ghost"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void anUnknownOrMalformedTokenIsA401Problem() throws Exception {
        for (String header : List.of("Bearer not-a-real-token", "Bearer ", "Basic YWJjOmRlZg==", "garbage")) {
            mvc.perform(get("/api/v1/players/me").header("Authorization", header))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void logoutEndsThatDevicesSessionOnly() throws Exception {
        String name = players.register(TestPlayers.unique("dev"));
        String phone = players.login(name);
        String laptop = players.login(name);
        mvc.perform(delete("/api/v1/sessions/current").header("Authorization", bearer(phone)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(phone))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(laptop))).andExpect(status().isOk());
    }

    @Test
    void listsDevicesAndRevokesAnotherOneAtOnce() throws Exception {
        String name = players.register(TestPlayers.unique("eli"));
        String phone = players.login(name);
        String lost = players.login(name);

        String devices = mvc.perform(get("/api/v1/players/me/devices").header("Authorization", bearer(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        List<Integer> current = JsonPath.read(devices, "$[?(@.current == true)].id");
        List<Integer> others = JsonPath.read(devices, "$[?(@.current == false)].id");
        assertThat(current).hasSize(1);

        mvc.perform(delete("/api/v1/players/me/devices/{id}", others.getFirst()).header("Authorization", bearer(phone)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(lost))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(phone))).andExpect(status().isOk());
    }

    @Test
    void cannotRevokeSomeoneElsesDevice() throws Exception {
        String mine = players.newPlayerToken("fay");
        String theirs = players.newPlayerToken("gus");
        String theirDevices = mvc.perform(get("/api/v1/players/me/devices").header("Authorization", bearer(theirs)))
                .andReturn().getResponse().getContentAsString();
        Integer theirSession = JsonPath.<List<Integer>>read(theirDevices, "$[*].id").getFirst();
        mvc.perform(delete("/api/v1/players/me/devices/{id}", theirSession).header("Authorization", bearer(mine)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(theirs))).andExpect(status().isOk());
    }
}
