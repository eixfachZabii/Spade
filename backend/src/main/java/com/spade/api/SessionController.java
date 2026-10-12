package com.spade.api;

import com.spade.api.dto.LoginRequest;
import com.spade.api.dto.SessionResponse;
import com.spade.service.SessionService;
import com.spade.service.SpadePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "sessions")
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @Operation(operationId = "login")
    @PostMapping
    @SecurityRequirements // public: no session needed (keep in step with SecurityConfig)
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return SessionResponse.from(sessions.login(request.username(), request.password(), request.deviceName(),
                ClientAddress.of(httpRequest)));
    }

    @Operation(operationId = "logout")
    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal SpadePrincipal me) {
        sessions.logout(me);
    }
}
