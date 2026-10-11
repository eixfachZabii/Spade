package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;
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

    private void wrongInvites(int times, String address) {
        for (int i = 0; i < times; i++) {
            throttle.inviteFailure(address);
        }
    }

    @Test
    void locksAnAddressAfterFiveWrongInviteCodesAndLetsItBackInAfterTheLockout() {
        wrongInvites(4, "5.5.5.5");
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
        wrongInvites(1, "5.5.5.5");
        assertThatThrownBy(() -> throttle.checkInvite("5.5.5.5"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("invite codes").hasMessageContaining("5 min")
                .satisfies(e -> assertThat(((DomainException) e).kind())
                        .isEqualTo(DomainException.Kind.TOO_MANY_REQUESTS));
        assertThatCode(() -> throttle.checkInvite("8.8.8.8")).doesNotThrowAnyException();
        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void aRightInviteCodeClearsThatAddressesInviteStrikes() {
        wrongInvites(4, "5.5.5.5");
        throttle.inviteSuccess("5.5.5.5");
        wrongInvites(4, "5.5.5.5");
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void wrongInviteCodesNeverLockLoginsFromThatAddress() {
        wrongInvites(5, "5.5.5.5");
        assertThatCode(() -> throttle.check("ana", "5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void wrongPasswordsNeverLockRegistrationFromThatAddress() {
        fail(5, "ana", "5.5.5.5");
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginDoesNotClearInviteStrikes() {
        wrongInvites(5, "5.5.5.5");
        throttle.success("ana", "5.5.5.5");
        assertThatThrownBy(() -> throttle.checkInvite("5.5.5.5")).isInstanceOf(DomainException.class);
    }

    @Test
    void forgetAllClearsLoginAndInviteStrikes() {
        fail(5, "ana", "5.5.5.5");
        wrongInvites(5, "5.5.5.5");
        throttle.forgetAll();
        assertThatCode(() -> throttle.check("ana", "5.5.5.5")).doesNotThrowAnyException();
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }
}
