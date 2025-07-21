// src/main/java/com/pokerapp/api/controller/GameController.java
package com.spadeboot.api.controller;

import com.spadeboot.service.GameService;
import com.spadeboot.service.UserService;
import com.spadeboot.api.dto.GameStateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing poker games.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    @PostMapping("/tables/{tableId}/start")
    public ResponseEntity<?> startGame(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "20") int bigBlind) {

        try {
            Long userId = userService.getCurrentUser().getId();
            GameStateDto gameState = gameService.startGame(tableId, userId, bigBlind);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Game started successfully");
            response.put("gameState", gameState);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/tables/{tableId}/end")
    public ResponseEntity<?> endGame(@PathVariable Long tableId) {
        try {
            Long userId = userService.getCurrentUser().getId();
            gameService.endGame(tableId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Game ended successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/tables/{tableId}/status")
    public ResponseEntity<?> getGameStatus(@PathVariable Long tableId) {
        try {
            GameStateDto gameState = gameService.getGameState(tableId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("gameState", gameState);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("hasActiveGame", false);
            return ResponseEntity.ok(error); // Return 200 with error info
        }
    }
}