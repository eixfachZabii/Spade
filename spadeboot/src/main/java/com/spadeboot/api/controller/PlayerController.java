package com.spadeboot.api.controller;

import com.spadeboot.api.dto.response.PlayerDto;
import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.User;
import com.spadeboot.exception.NotFoundException;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.service.UserService;
import com.spadeboot.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TableService tableService;


    @GetMapping("/me")
    public ResponseEntity<PlayerDto> getCurrentPlayer() {
        Long userId = userService.getCurrentUser().getId();
        Player player = playerRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Error fetching Player with user ID: " + userId));
        PlayerDto playerDto = convertToDto(player);
        return ResponseEntity.ok(playerDto);
    }


    /**
     * Check if the current user is at a table
     * @return TableStatus object with tableId if at table, or null if not
     */
    @GetMapping("/current-table")
    public ResponseEntity<Map<String, Object>> getCurrentTable() {
        User currentUser = userService.getCurrentUser();
        Map<String, Object> response = new HashMap<>();

        if (currentUser != null) {
            Optional<Player> playerOpt = playerRepository.findByUserId(currentUser.getId());

            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                Long tableId = player.getCurrentTableId();

                response.put("isAtTable", tableId != null);
                response.put("tableId", tableId);

                // If at a table, include basic table info
                if (tableId != null) {
                    try {
                        response.put("table", tableService.getTableById(tableId));
                    } catch (Exception e) {
                        // Table might have been deleted or other error
                        response.put("table", null);
                    }
                }
            } else {
                response.put("isAtTable", false);
                response.put("tableId", null);
            }
        } else {
            response.put("isAtTable", false);
            response.put("tableId", null);
        }

        return ResponseEntity.ok(response);
    }

    private PlayerDto convertToDto(Player player) {
        PlayerDto dto = new PlayerDto();
        dto.setId(player.getId());
        dto.setUser(player.getUser());
        dto.setChips(player.getChips());
        dto.setStatus(player.getStatus());
        dto.setCurrentTableId(player.getCurrentTableId());
        dto.setWinProbability(player.getWinProbability());
        dto.setTotalBet(player.getTotalBet());

        return dto;
    }
}