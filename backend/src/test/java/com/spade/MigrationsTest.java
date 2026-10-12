package com.spade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpadeIntegrationTest
class MigrationsTest {

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyMigrationAppliesToAnEmptyDatabase() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isApplied()).as(m.getScript()).isTrue());
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void theDatabaseRefusesAnUpperCaseUsername() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into player (username, display_name, password_hash, created_at) values ('MixedCase', 'M', 'x', now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRefusesASecondOpenTableForOneHost() {
        jdbc.update("""
                insert into player (username, display_name, password_hash, created_at) values ('onehost', 'H', 'x', now())""");
        Long host = jdbc.queryForObject("select id from player where username = 'onehost'", Long.class);
        String insert = """
                insert into poker_table (name, host_id, small_blind, big_blind, max_seats, opened_at) values (?, ?, 5, 10, 6, now())""";
        jdbc.update(insert, "first", host);
        assertThatThrownBy(() -> jdbc.update(insert, "second", host)).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("update poker_table set closed_at = now() where host_id = ?", host);
        jdbc.update(insert, "third", host);   // a closed table does not count
    }
}
