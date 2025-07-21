// src/main/java/com/pokerapp/api/controller/TableController.java
package com.spadeboot.api.controller;

import com.spadeboot.api.dto.request.TableSettingsDto;
import com.spadeboot.api.dto.response.TableDto;
import com.spadeboot.domain.user.User;
import com.spadeboot.service.TableService;
import com.spadeboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private TableService tableService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<TableDto> createTable(@Valid @RequestBody TableSettingsDto settings) {
        User currentUser = userService.getCurrentUser();
        userService.createPlayer(currentUser.getId());
        TableDto table = tableService.createTable(settings, currentUser);
        return ResponseEntity.ok(table);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TableDto> deleteTable(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(tableService.deleteTable(id, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<TableDto>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/public")
    public ResponseEntity<List<TableDto>> getPublicTables() {
        return ResponseEntity.ok(tableService.getPublicTables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableDto> getTableById(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.getTableById(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, Object>> joinTable(@PathVariable Long id, @RequestParam Integer buyIn) {
        // Get the current user
        Long userId = userService.getCurrentUser().getId();
        userService.createPlayer(userId);

        // Join the table through the service
        TableDto tableDto = tableService.joinTable(id, userId, buyIn);

        // Return table data along with WebSocket connection info
        Map<String, Object> response = new HashMap<>();
        response.put("table", tableDto);
        response.put("userId", userId);
        response.put("tableId", id);
        response.put("websocketEndpoint", "/ws");
        response.put("tableTopic", "/topic/tables/" + id);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/leave")
    public ResponseEntity<TableDto> leaveTable(@PathVariable Long id) {
        // Pass the User ID, not the Player ID
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(tableService.leaveTable(id, userId));
    }
}