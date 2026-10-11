package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ADR 0011's promise: revoking a lost phone takes effect at once. A request that is touching the session
 * (last_used_at) while a revoke commits must not write the old revoked_at (NULL) back.
 */
@SpadeIntegrationTest
class SessionRevokeRaceTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SessionService sessions;

    @Autowired
    PlatformTransactionManager transactions;

    @Test
    void aRevokeCommittedWhileAnotherRequestTouchesTheSessionStaysRevoked() throws Exception {
        String name = "race_" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"%s\",\"displayName\":\"%s\",\"password\":\"correct-horse\",\"inviteCode\":\"letmein\"}"
                        .formatted(name, name))).andExpect(status().isCreated());
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"%s\",\"password\":\"correct-horse\",\"deviceName\":\"race phone\"}".formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        long sessionId = jdbc.queryForObject("""
                select s.id from device_session s join player p on p.id = s.player_id where p.username = ?""",
                Long.class, name);
        long playerId = jdbc.queryForObject("select player_id from device_session where id = ?", Long.class, sessionId);
        // Last used long ago, so that authenticating has to touch it.
        jdbc.update("update device_session set last_used_at = now() - interval '5 minutes' where id = ?", sessionId);

        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            // Request A: authenticates (loads the session, touches it) and is still open ...
            new TransactionTemplate(transactions).executeWithoutResult(a -> {
                assertThat(sessions.authenticate(token)).isPresent();
                // ... when request B revokes the same session and commits.
                try {
                    other.submit(() -> sessions.revoke(playerId, sessionId)).get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }); // A commits here, flushing its touch.
        } finally {
            other.shutdownNow();
        }

        List<Object> revokedAt = jdbc.queryForList("select revoked_at from device_session where id = ?", Object.class, sessionId);
        assertThat(revokedAt).singleElement().isNotNull();
        assertThat(sessions.authenticate(token)).isEmpty();
    }
}
