package com.spade.testworld;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps the testworld away from real data. A database is testworld-safe when it has no players, or when every
 * player was created at the testworld's frozen instant: only the testworld's clock writes that instant.
 * No schema marker on purpose: Flyway migrations ship to production. Never reports row contents.
 */
@Component
@Profile("testworld")
class TestworldGuard {

    static final String REFUSAL = "The testworld refuses to run on a database with real players. "
            + "Point SPADE_DB_URL at a throwaway database, or run "
            + "./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld.";

    private final JdbcTemplate jdbc;

    TestworldGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    boolean isTestworldSafe() {
        Integer others = jdbc.queryForObject("select count(*) from player where created_at <> ?", Integer.class,
                OffsetDateTime.ofInstant(TestworldClock.NOW, ZoneOffset.UTC));
        return others != null && others == 0;
    }
}
