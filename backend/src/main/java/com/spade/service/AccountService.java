package com.spade.service;

import com.spade.domain.DomainException;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Accounts: one per person, opened with the group's invite code (phase D2). */
@Service
public class AccountService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final PlayerRepository players;
    private final PasswordEncoder passwords;
    private final SpadeProperties settings;
    private final Clock clock;
    private final LoginThrottle throttle;

    public AccountService(PlayerRepository players, PasswordEncoder passwords, SpadeProperties settings, Clock clock,
            LoginThrottle throttle) {
        this.players = players;
        this.passwords = passwords;
        this.settings = settings;
        this.clock = clock;
        this.throttle = throttle;
    }

    @Transactional
    public PlayerView register(String username, String displayName, String password, String inviteCode,
            String address) {
        throttle.checkInvite(address);
        if (!Hashing.matchesSha256(inviteCode, settings.inviteCodeSha256())) {
            throttle.inviteFailure(address);
            throw new DomainException(DomainException.Kind.FORBIDDEN,
                    "That invite code isn't ours. Ask the host for the group's code.");
        }
        throttle.inviteSuccess(address);
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "Passwords can be at most 72 bytes long. Choose a shorter one.");
        }
        String name = normalize(username);
        if (players.existsByUsername(name)) {
            throw new DomainException(DomainException.Kind.CONFLICT, "The username '" + name + "' is taken.");
        }
        PlayerEntity saved = players.save(
                new PlayerEntity(name, displayName.strip(), passwords.encode(password), clock.instant()));
        return PlayerView.of(saved);
    }

    @Transactional(readOnly = true)
    public PlayerView get(long playerId) {
        return players.findById(playerId).map(PlayerView::of)
                .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "No such player."));
    }

    static String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }
}
