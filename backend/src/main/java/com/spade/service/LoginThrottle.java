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
 * from are locked for {@code lockout}. In memory; a restart forgets (fine for one PC, ADR 0007).
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
        Instant now = clock.instant();
        for (String key : keys(username, address)) {
            Strikes s = strikes.get(key);
            if (s != null && s.lockedUntil() != null && now.isBefore(s.lockedUntil())) {
                long minutes = (Duration.between(now, s.lockedUntil()).toSeconds() + 59) / 60;
                throw new DomainException(DomainException.Kind.TOO_MANY_REQUESTS,
                        "Too many wrong logins. Try again in " + minutes + " min.");
            }
        }
    }

    public void failure(String username, String address) {
        Instant now = clock.instant();
        for (String key : keys(username, address)) {
            strikes.compute(key, (k, s) -> {
                boolean fresh = s == null || (s.lockedUntil() != null && !now.isBefore(s.lockedUntil()));
                int failures = fresh ? 1 : s.failures() + 1;
                return failures >= settings.login().maxFailures()
                        ? new Strikes(0, now.plus(settings.login().lockout()))
                        : new Strikes(failures, null);
            });
        }
    }

    public void success(String username, String address) {
        keys(username, address).forEach(strikes::remove);
    }

    /** For the testworld's reset only. */
    public void forgetAll() {
        strikes.clear();
    }

    private static List<String> keys(String username, String address) {
        return List.of("user:" + username, "addr:" + address);
    }
}
