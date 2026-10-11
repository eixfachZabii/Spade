package com.spade.api;

import com.spade.api.dto.DeviceResponse;
import com.spade.api.dto.PlayerResponse;
import com.spade.api.dto.RegisterPlayerRequest;
import com.spade.service.AccountService;
import com.spade.service.SessionService;
import com.spade.service.SpadePrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final AccountService accounts;
    private final SessionService sessions;

    public PlayerController(AccountService accounts, SessionService sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        return PlayerResponse.from(accounts.register(
                request.username(), request.displayName(), request.password(), request.inviteCode()));
    }

    @GetMapping("/me")
    public PlayerResponse me(@AuthenticationPrincipal SpadePrincipal me) {
        return PlayerResponse.from(accounts.get(me.playerId()));
    }

    @GetMapping("/me/devices")
    public List<DeviceResponse> devices(@AuthenticationPrincipal SpadePrincipal me) {
        return sessions.devices(me.playerId()).stream().map(d -> DeviceResponse.from(d, me.sessionId())).toList();
    }

    @DeleteMapping("/me/devices/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long sessionId) {
        sessions.revoke(me.playerId(), sessionId);
    }
}
