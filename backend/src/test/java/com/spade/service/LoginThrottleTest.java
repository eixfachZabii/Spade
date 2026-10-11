package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.MutableClock;
import com.spade.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Limits under test: 5 failures per username, 8 per address (the shipped address limit is 20), lockout 5 min. */
class LoginThrottleTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-09T19:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(
            new SpadeProperties("0".repeat(64), new SpadeProperties.Login(5, 8, Duration.ofMinutes(5))), clock);

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
        for (int i = 0; i < 7; i++) {
            fail(1, "guess" + i, "6.6.6.6");
        }
        assertThatCode(() -> throttle.check("f", "6.6.6.6")).doesNotThrowAnyException();
        fail(1, "guess7", "6.6.6.6");
        assertThatThrownBy(() -> throttle.check("f", "6.6.6.6")).isInstanceOf(DomainException.class);
        assertThatCode(() -> throttle.check("f", "7.7.7.7")).doesNotThrowAnyException();
    }

    @Test
    void anAddressStaysOpenWhileEachUsernameHasFewerThanFiveStrikes() {
        fail(4, "ana", "6.6.6.6");
        assertThatCode(() -> throttle.check("ben", "6.6.6.6")).doesNotThrowAnyException();
        fail(1, "ana", "6.6.6.6");
        assertThatThrownBy(() -> throttle.check("ana", "6.6.6.6")).isInstanceOf(DomainException.class);
        assertThatCode(() -> throttle.check("ben", "6.6.6.6")).doesNotThrowAnyException();
    }

    @Test
    void aLateLoginFailureWhileLockedKeepsTheUsernameLock() {
        fail(5, "ana", "1.1.1.1");
        fail(1, "ana", "1.1.1.1");
        assertThatThrownBy(() -> throttle.check("ana", "9.9.9.9")).isInstanceOf(DomainException.class);
        clock.advance(Duration.ofMinutes(4));
        fail(1, "ana", "1.1.1.1");
        assertThatThrownBy(() -> throttle.check("ana", "9.9.9.9")).isInstanceOf(DomainException.class);
        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> throttle.check("ana", "9.9.9.9")).doesNotThrowAnyException();
    }

    @Test
    void aLateLoginFailureWhileLockedKeepsTheAddressLock() {
        for (int i = 0; i < 8; i++) {
            fail(1, "guess" + i, "3.3.3.3");
        }
        fail(1, "late", "3.3.3.3");
        assertThatThrownBy(() -> throttle.check("fresh", "3.3.3.3")).isInstanceOf(DomainException.class);
        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> throttle.check("fresh", "3.3.3.3")).doesNotThrowAnyException();
    }

    @Test
    void aLateInviteFailureWhileLockedKeepsTheLock() {
        wrongInvites(8, "4.4.4.4");
        wrongInvites(1, "4.4.4.4");
        assertThatThrownBy(() -> throttle.checkInvite("4.4.4.4")).isInstanceOf(DomainException.class);
        clock.advance(Duration.ofMinutes(4));
        wrongInvites(1, "4.4.4.4");
        assertThatThrownBy(() -> throttle.checkInvite("4.4.4.4")).isInstanceOf(DomainException.class);
        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> throttle.checkInvite("4.4.4.4")).doesNotThrowAnyException();
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
    void locksAnAddressAfterEightWrongInviteCodesAndLetsItBackInAfterTheLockout() {
        wrongInvites(7, "5.5.5.5");
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
        wrongInvites(7, "5.5.5.5");
        throttle.inviteSuccess("5.5.5.5");
        wrongInvites(7, "5.5.5.5");
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void wrongInviteCodesNeverLockLoginsFromThatAddress() {
        wrongInvites(8, "5.5.5.5");
        assertThatCode(() -> throttle.check("ana", "5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void wrongPasswordsNeverLockRegistrationFromThatAddress() {
        for (int i = 0; i < 8; i++) {
            fail(1, "guess" + i, "5.5.5.5");
        }
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginDoesNotClearInviteStrikes() {
        wrongInvites(8, "5.5.5.5");
        throttle.success("ana", "5.5.5.5");
        assertThatThrownBy(() -> throttle.checkInvite("5.5.5.5")).isInstanceOf(DomainException.class);
    }

    @Test
    void forgetAllClearsLoginAndInviteStrikes() {
        fail(5, "ana", "5.5.5.5");
        wrongInvites(8, "5.5.5.5");
        throttle.forgetAll();
        assertThatCode(() -> throttle.check("ana", "5.5.5.5")).doesNotThrowAnyException();
        assertThatCode(() -> throttle.checkInvite("5.5.5.5")).doesNotThrowAnyException();
    }
}
