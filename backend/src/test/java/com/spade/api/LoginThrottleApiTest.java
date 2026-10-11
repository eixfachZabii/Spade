package com.spade.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Behind the Cloudflare Tunnel every request has the same remote address; the throttle keys on CF-Connecting-IP. */
@SpadeIntegrationTest
class LoginThrottleApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions login(String user, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/sessions").header("CF-Connecting-IP", ip)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","password":"%s","deviceName":"x"}""".formatted(user, password)));
    }

    @Test
    void oneTypistDoesNotLockOutTheGroup() throws Exception {
        TestPlayers players = new TestPlayers(mvc);
        String clumsy = players.register(TestPlayers.unique("clumsy"));
        String friend = players.register(TestPlayers.unique("friend"));

        for (int i = 0; i < 5; i++) {
            login(clumsy, "wrong-password", "203.0.113.7").andExpect(status().isUnauthorized());
        }
        login(clumsy, TestPlayers.PASSWORD, "203.0.113.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        login(friend, TestPlayers.PASSWORD, "198.51.100.23").andExpect(status().isCreated());
    }
}
