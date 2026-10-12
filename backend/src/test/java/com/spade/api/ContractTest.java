package com.spade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.TestworldTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API is a contract between backend, hub and app (ADR 0012, phase D6). A changed response fails here
 * until it is regenerated on purpose:  SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest  — then commit api/.
 */
@TestworldTest
class ContractTest {

    private static final Path API = Path.of("..", "api");
    private static final boolean UPDATE = "1".equals(System.getenv("SPADE_CONTRACT_UPDATE"));
    private static final JsonMapper PRETTY = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void freshWorld() throws Exception {
        mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
    }

    @Test
    void theOpenApiSpecMatchesTheCommittedOne() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        compare("openapi.json", spec);
    }

    @Test
    void theFixturesMatchTheCommittedOnes() throws Exception {
        String ana = login("ana");
        String ben = login("ben");
        sitDown(ana, 1000);
        sitDown(ben, 1000);
        compare("fixtures/player-me.json", pretty(getAs(ana, "/api/v1/players/me")));
        compare("fixtures/devices.json", pretty(getAs(ana, "/api/v1/players/me/devices")));
        compare("fixtures/tables.json", pretty(getAs(ana, "/api/v1/tables")));
        compare("fixtures/table.json", pretty(getAs(ana, "/api/v1/tables/1")));
    }

    private String login(String name) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"testworld","deviceName":"%s's iPhone"}""".formatted(name, name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private void sitDown(String token, long buyIn) throws Exception {
        mvc.perform(post("/api/v1/tables/1/seats").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"buyIn\":" + buyIn + "}"))
                .andExpect(status().isCreated());
    }

    private String getAs(String token, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String pretty(String json) {
        return PRETTY.writeValueAsString(PRETTY.readTree(json));
    }

    private static void compare(String name, String body) throws Exception {
        Path file = API.resolve(name);
        String normalized = body.strip() + "\n";
        if (UPDATE) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, normalized);
            return;
        }
        assertThat(file).as("api/%s is missing. Generate it: SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest", name)
                .exists();
        assertThat(normalized)
                .as("The API changed (api/%s). If that was on purpose: SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest, review the diff, commit api/", name)
                .isEqualTo(Files.readString(file));
    }
}
