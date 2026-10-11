package com.spade.service;

import com.spade.domain.DomainException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing: after {@code maxFailures} wrong logins, a username and the address they came
 * from are locked for {@code lockout}. Wrong invite codes at registration strike the caller's address the same
 * way, under their own keys, so the group's code can't be guessed through the tunnel. In memory; a restart forgets (fine for one PC, ADR 0007).
 */
@Component
public class LoginThrottle {

    private record Strikes(int failures, Instant lockedUntil) {
    }

    private final Map<String, Strikes> strikes = new ConcurrentHashMap<>();
    private final SpadeProperties settings;
    private final Clock clock;

    public LoginThrottle(SpadeProperties settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public void check(String username, String address) {
        requireNotLocked(keys(username, address), "wrong logins");
    }

    public void failure(String username, String address) {
        keys(username, address).forEach(this::strike);
    }

    public void success(String username, String address) {
        keys(username, address).forEach(strikes::remove);
    }

    /** Registration: wrong invite codes are counted per caller address, apart from the login strikes. */
    public void checkInvite(String address) {
        requireNotLocked(List.of(inviteKey(address)), "wrong invite codes");
    }

    public void inviteFailure(String address) {
        strike(inviteKey(address));
    }

    public void inviteSuccess(String address) {
        strikes.remove(inviteKey(address));
    }

    private void requireNotLocked(List<String> keys, String what) {
        Instant now = clock.instant();
        for (String key : keys) {
            Strikes s = strikes.get(key);
            if (s != null && s.lockedUntil() != null && now.isBefore(s.lockedUntil())) {
                long minutes = (Duration.between(now, s.lockedUntil()).toSeconds() + 59) / 60;
                throw new DomainException(DomainException.Kind.TOO_MANY_REQUESTS,
                        "Too many " + what + ". Try again in " + minutes + " min.");
            }
        }
    }

    private void strike(String key) {
        Instant now = clock.instant();
        strikes.compute(key, (k, s) -> {
            boolean fresh = s == null || (s.lockedUntil() != null && !now.isBefore(s.lockedUntil()));
            int failures = fresh ? 1 : s.failures() + 1;
            return failures >= settings.login().maxFailures()
                    ? new Strikes(0, now.plus(settings.login().lockout()))
                    : new Strikes(failures, null);
        });
    }

    /** For the testworld's reset only. */
    public void forgetAll() {
        strikes.clear();
    }

    private static List<String> keys(String username, String address) {
        return List.of("user:" + username, "addr:" + address);
    }

    private static String inviteKey(String address) {
        return "invite:" + address;
    }
}
