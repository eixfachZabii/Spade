package com.spade.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpadeIntegrationTest
class PlayerApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions register(String username, String password, String inviteCode) throws Exception {
        return mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","displayName":"Ana","password":"%s","inviteCode":"%s"}"""
                .formatted(username, password, inviteCode)));
    }

    @Test
    void registersWithTheGroupsInviteCode() throws Exception {
        String name = TestPlayers.unique("ana");
        register(name, "correct-horse", "letmein")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(content().string(not(containsString("correct-horse"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void refusesAWrongInviteCode() throws Exception {
        register(TestPlayers.unique("eve"), "correct-horse", "guess")
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("invite code")));
    }

    @Test
    void usernamesIgnoreCapitals() throws Exception {
        String name = TestPlayers.unique("Ana");
        register(name, "correct-horse", "letmein")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name.toLowerCase()));
        register(name.toUpperCase(), "correct-horse", "letmein").andExpect(status().isConflict());
    }

    @Test
    void refusesUsernamesWithSpacesOrSymbols() throws Exception {
        register("ana smith", "correct-horse", "letmein")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("username")))
                .andExpect(jsonPath("$.detail").value(containsString("use 3 to 32 letters, digits, '.', '_' or '-'")));
        register("an", "correct-horse", "letmein").andExpect(status().isBadRequest());
    }

    @Test
    void refusesPasswordsOutsideEightTo72Bytes() throws Exception {
        register(TestPlayers.unique("short"), "seven77", "letmein")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("password")))
                .andExpect(jsonPath("$.detail").value(containsString("8 to 72")))
                .andExpect(content().string(not(containsString("seven77"))));
        register(TestPlayers.unique("long"), "ü".repeat(40), "letmein")   // 40 characters, 80 bytes
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("72 bytes")));
    }

    @Test
    void protectedEndpointsNeedASession() throws Exception {
        mvc.perform(get("/api/v1/players/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
