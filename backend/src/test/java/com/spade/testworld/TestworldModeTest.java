package com.spade.testworld;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import com.spade.TestworldTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class TestworldModeTest {

    static String login(MockMvc mvc, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"testworld","deviceName":"testworld"}""".formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Nested
    @TestworldTest
    class InTheTestworld {

        @Autowired
        MockMvc mvc;

        @Test
        void hasItsPlayersTableAndClock() throws Exception {
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
            String ana = login(mvc, "ana");
            mvc.perform(get("/api/v1/tables").header("Authorization", "Bearer " + ana))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Testworld table"))
                    .andExpect(jsonPath("$[0].smallBlind").value(5));
            mvc.perform(get("/api/v1/players/me/devices").header("Authorization", "Bearer " + ana))
                    .andExpect(jsonPath("$[0].createdAt").value("2026-10-09T19:00:00Z"));
        }

        @Test
        void resetForgetsEverySession() throws Exception {
            String ben = login(mvc, "ben");
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + ben))
                    .andExpect(status().isUnauthorized());
            login(mvc, "ben");
        }
    }

    @Nested
    @SpadeIntegrationTest
    class OutsideTheTestworld {

        @Autowired
        MockMvc mvc;

        @Test
        void theResetDoesNotExist() throws Exception {
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNotFound());
        }
    }
}
