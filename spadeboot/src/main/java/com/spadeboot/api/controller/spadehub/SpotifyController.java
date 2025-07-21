// src/main/java/com/pokerapp/api/controller/SpotifyController.java
package com.spadeboot.api.controller.spadehub;

import com.spadeboot.service.spadehub.SpotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    @Autowired
    private SpotifyService spotifyService;

    @Value("${spotify.redirect-view}")
    private String redirectView;

    @GetMapping("/login")
    public RedirectView login() {
        String state = UUID.randomUUID().toString();
        String authUrl = spotifyService.createAuthorizationUrl(state);
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam(required = false) String code,
                                 @RequestParam(required = false) String error,
                                 @RequestParam String state) {
        if (error != null) {
            return new RedirectView(redirectView +"/spotify?error=" + error);
        }

        Map<String, Object> tokens = spotifyService.exchangeCodeForTokens(code);
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        Long expiresAt = (Long) tokens.get("expires_at");

        // Redirect to the FRONTEND (not backend) with tokens
        return new RedirectView(
                        redirectView +
                    "/spotify#access_token=" + accessToken +
                        "&refresh_token=" + refreshToken +
                        "&expires_at=" + expiresAt
        );
    }

    @GetMapping("/refresh_token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refresh_token) {
        Map<String, Object> tokens = spotifyService.refreshAccessToken(refresh_token);
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/lyrics")
    public ResponseEntity<Map<String, Object>> getLyrics(
            @RequestParam String artist,
            @RequestParam String title) {

        Map<String, Object> result = spotifyService.fetchLyrics(artist, title);

        if (result.containsKey("error")) {
            return ResponseEntity.status(404).body(result);
        }

        return ResponseEntity.ok(result);
    }
}