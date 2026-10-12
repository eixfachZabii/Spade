package com.spade.testworld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.TestworldTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The testworld must never wipe, or write frozen timestamps into, a database that has real players. */
class TestworldGuardTest {

    static final String REAL_PLAYER = """
            insert into player (username, display_name, password_hash, created_at)
            values ('mallory', 'Mallory', 'x', timestamptz '2026-01-01 12:00:00+00')""";

    @Test
    void startupRefusesADatabaseWithARealPlayer() {
        try (PostgreSQLContainer db = new PostgreSQLContainer("postgres:18-alpine")) {
            db.start();
            Flyway.configure().dataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword()).load().migrate();
            new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                    db.getJdbcUrl(), db.getUsername(), db.getPassword())).execute(REAL_PLAYER);

            assertThatThrownBy(() -> new SpringApplicationBuilder(com.spade.SpadeApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .profiles("testworld")
                    .run("--server.port=0", "--spring.datasource.url=" + db.getJdbcUrl(),
                            "--spring.datasource.username=" + db.getUsername(),
                            "--spring.datasource.password=" + db.getPassword())
                    .close())
                    .hasStackTraceContaining("SPADE_DB_URL")
                    .hasStackTraceContaining("refuses to run on a database with real players");
        }
    }

    @Nested
    @TestworldTest
    class Reset {

        @Autowired
        MockMvc mvc;
        @Autowired
        JdbcTemplate jdbc;

        @Test
        void refusesAndKeepsTheRealPlayer() throws Exception {
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
            jdbc.execute(REAL_PLAYER);
            try {
                mvc.perform(post("/api/v1/testworld/reset"))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("SPADE_DB_URL")));
                assertThat(jdbc.queryForObject("select count(*) from player where username = 'mallory'",
                        Integer.class)).isEqualTo(1);
                assertThat(jdbc.queryForObject("select count(*) from player", Integer.class)).isEqualTo(7);
            } finally {
                jdbc.update("delete from player where username = 'mallory'");
            }
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
        }
    }
}
