package com.spade;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpadeIntegrationTest
class MigrationsTest {

    @Autowired
    Flyway flyway;

    @Test
    void everyMigrationAppliesToAnEmptyDatabase() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isApplied()).as(m.getScript()).isTrue());
        assertThat(flyway.info().pending()).isEmpty();
    }
}
