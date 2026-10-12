package com.spade.testworld;

import com.spade.domain.DomainException;
import com.spade.service.LoginThrottle;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exists only in the testworld; hidden from the OpenAPI contract. Puts the world back to its seed. */
@Hidden
@RestController
@Profile("testworld")
@RequestMapping("/api/v1/testworld")
class TestworldController {

    private final TestworldGuard guard;
    private final JdbcTemplate jdbc;
    private final TestworldSeed seed;
    private final LoginThrottle throttle;

    TestworldController(TestworldGuard guard, JdbcTemplate jdbc, TestworldSeed seed, LoginThrottle throttle) {
        this.guard = guard;
        this.jdbc = jdbc;
        this.seed = seed;
        this.throttle = throttle;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        // 409, not 403: the caller may do this in a testworld, the database's state is what forbids it
        if (!guard.isTestworldSafe()) {
            throw new DomainException(DomainException.Kind.CONFLICT, TestworldGuard.REFUSAL);
        }
        jdbc.execute("truncate table cash_out, buy_in, seat, poker_table, device_session, player restart identity cascade");
        throttle.forgetAll();
        seed.seed();
    }
}
