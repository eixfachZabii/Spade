package com.spade.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.MutableClock;
import com.spade.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-09T19:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(
            new SpadeProperties("0".repeat(64), new SpadeProperties.Login(5, Duration.ofMinutes(5))), clock);

    private void fail(int times, String user, String address) {
        for (int i = 0; i < times; i++) {
            throttle.failure(user, address);
        }
    }

    @Test
    void locksAUsernameAfterFiveFailuresForFiveMinutes() {
        fail(5, "ana", "1.1.1.1");
        assertThatThrownBy(() -> throttle.check("ana", "9.9.9.9"))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 min");
        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> throttle.check("ana", "9.9.9.9")).doesNotThrowAnyException();
    }

    @Test
    void locksAnAddressThatGuessesManyUsernames() {
        fail(1, "a", "6.6.6.6");
        fail(1, "b", "6.6.6.6");
        fail(1, "c", "6.6.6.6");
        fail(1, "d", "6.6.6.6");
        fail(1, "e", "6.6.6.6");
        assertThatThrownBy(() -> throttle.check("f", "6.6.6.6")).isInstanceOf(DomainException.class);
        assertThatCode(() -> throttle.check("f", "7.7.7.7")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginClearsTheStrikes() {
        fail(4, "ben", "2.2.2.2");
        throttle.success("ben", "2.2.2.2");
        fail(4, "ben", "2.2.2.2");
        assertThatCode(() -> throttle.check("ben", "2.2.2.2")).doesNotThrowAnyException();
    }
}
