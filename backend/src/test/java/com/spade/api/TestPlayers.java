package com.spade.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Registers and logs in players through the API, with names unique per test run. Invite code: letmein. */
final class TestPlayers {

    static final String PASSWORD = "correct-horse";
    private final MockMvc mvc;

    TestPlayers(MockMvc mvc) {
        this.mvc = mvc;
    }

    static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    String register(String username) throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","displayName":"%s","password":"%s","inviteCode":"letmein"}"""
                .formatted(username, username, PASSWORD))).andExpect(status().isCreated());
        return username;
    }

    String login(String username) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s","deviceName":"test phone"}""".formatted(username, PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    String newPlayerToken(String prefix) throws Exception {
        return login(register(unique(prefix)));
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
