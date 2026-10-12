package com.spade.service;

import com.spade.domain.DomainException;
import com.spade.repository.DeviceSessionEntity;
import com.spade.repository.DeviceSessionRepository;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Device sessions: log in once per device, revoke at once (ADR 0011). */
@Service
public class SessionService {

    private final PlayerRepository players;
    private final DeviceSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final LoginThrottle throttle;
    private final String decoyHash;

    public SessionService(PlayerRepository players, DeviceSessionRepository sessions, PasswordEncoder passwords,
            Clock clock, LoginThrottle throttle) {
        this.players = players;
        this.sessions = sessions;
        this.passwords = passwords;
        this.clock = clock;
        this.throttle = throttle;
        this.decoyHash = passwords.encode("decoy-for-unknown-usernames");
    }

    @Transactional
    public LoginResult login(String username, String password, String deviceName, String address) {
        String name = AccountService.normalize(username);
        throttle.check(name, address);
        Optional<PlayerEntity> player = players.findByUsername(name);
        // Check a decoy hash for unknown names too, so timing doesn't reveal who has an account.
        boolean passwordMatches = passwords.matches(password, player.map(PlayerEntity::getPasswordHash).orElse(decoyHash));
        if (player.isEmpty() || !passwordMatches) {
            throttle.failure(name, address);
            throw new DomainException(DomainException.Kind.UNAUTHORIZED, "Wrong username or password.");
        }
        throttle.success(name, address);
        String token = SessionTokens.newToken();
        DeviceSessionEntity session = sessions.save(new DeviceSessionEntity(
                player.get().getId(), Hashing.sha256Hex(token), deviceName.strip(), clock.instant()));
        return new LoginResult(token, SessionView.of(session));
    }

    @Transactional
    public Optional<SpadePrincipal> authenticate(String token) {
        return sessions.findByTokenSha256AndRevokedAtIsNull(Hashing.sha256Hex(token)).map(session -> {
            session.touch(clock.instant());
            return new SpadePrincipal(session.getPlayerId(), session.getId());
        });
    }

    @Transactional
    public void logout(SpadePrincipal me) {
        revoke(me.playerId(), me.sessionId());
    }

    @Transactional(readOnly = true)
    public List<SessionView> devices(long playerId) {
        return sessions.findByPlayerIdAndRevokedAtIsNullOrderByCreatedAtAscIdAsc(playerId).stream().map(SessionView::of).toList();
    }

    @Transactional
    public void revoke(long playerId, long sessionId) {
        Instant now = clock.instant();
        sessions.findByIdAndPlayerIdAndRevokedAtIsNull(sessionId, playerId)
                .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "You have no such device."))
                .revoke(now);
    }
}
