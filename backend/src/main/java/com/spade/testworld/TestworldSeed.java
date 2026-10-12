package com.spade.testworld;

import com.spade.service.AccountService;
import com.spade.service.PlayerView;
import com.spade.service.SpadePrincipal;
import com.spade.service.TableService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Seeds six players (password "testworld") and one table hosted by ana, blinds 5/10. */
@Component
@Profile("testworld")
class TestworldSeed implements ApplicationRunner {

    static final List<String> PLAYERS = List.of("ana", "ben", "cleo", "dev", "eli", "fay");

    private static final Logger log = LoggerFactory.getLogger(TestworldSeed.class);

    private final TestworldGuard guard;
    private final AccountService accounts;
    private final TableService tables;
    private final JdbcTemplate jdbc;

    TestworldSeed(TestworldGuard guard, AccountService accounts, TableService tables, JdbcTemplate jdbc) {
        this.guard = guard;
        this.accounts = accounts;
        this.tables = tables;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("testworld active: fixed clock, open reset — never use it with real data");
        if (!guard.isTestworldSafe()) {
            throw new IllegalStateException(TestworldGuard.REFUSAL);
        }
        Integer existing = jdbc.queryForObject("select count(*) from player", Integer.class);
        if (existing == null || existing == 0) {
            seed();
        }
    }

    void seed() {
        PlayerView host = null;
        for (String name : PLAYERS) {
            PlayerView player = accounts.register(name, Character.toUpperCase(name.charAt(0)) + name.substring(1),
                    "testworld", "testworld", "testworld-seed");
            if (host == null) {
                host = player;
            }
        }
        tables.open(new SpadePrincipal(host.id(), 0), "Testworld table", 5, 10, 6);
    }
}
