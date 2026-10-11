package com.spade.api;

import com.spade.api.dto.PlayerResponse;
import com.spade.api.dto.RegisterPlayerRequest;
import com.spade.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final AccountService accounts;

    public PlayerController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        return PlayerResponse.from(accounts.register(
                request.username(), request.displayName(), request.password(), request.inviteCode()));
    }
}
