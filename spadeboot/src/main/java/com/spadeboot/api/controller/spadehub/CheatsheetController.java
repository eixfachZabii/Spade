// src/main/java/com/pokerapp/api/controller/spadehub/CheatsheetController.java
package com.spadeboot.api.controller.spadehub;

import com.spadeboot.api.dto.request.ChipInventoryDto;
import com.spadeboot.api.dto.response.ChipDistributionDto;
import com.spadeboot.service.spadehub.CheatsheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cheatsheet")
public class CheatsheetController {

    @Autowired
    private CheatsheetService cheatsheetService;

    @GetMapping("/heatmap")
    public ResponseEntity<List<CheatsheetService.HeatmapDataPoint>> getHeatmapData() {
        return ResponseEntity.ok(cheatsheetService.getHeatmapData());
    }

    @PostMapping("/chips/optimize")
    public ResponseEntity<ChipDistributionDto> calculateOptimalDistribution(
            @RequestBody ChipInventoryDto chipInventory) {

        ChipDistributionDto result =
                cheatsheetService.calculateOptimalChipDistribution(chipInventory);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/chips/presets")
    public ResponseEntity<?> getPresetDistributions(@RequestParam(required = false) Integer players) {
        // Simplified implementation with presets for different numbers of players
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Preset distributions available",
                "presets", List.of(
                        Map.of(
                                "name", "Tournament Style",
                                "players", players != null ? players : 8,
                                "valuePerPlayer", 1000,
                                "distribution", Map.of(
                                        "chip1", 0,
                                        "chip5", 10,
                                        "chip10", 10,
                                        "chip25", 8,
                                        "chip100", 5,
                                        "chip500", 0
                                )
                        ),
                        Map.of(
                                "name", "Cash Game",
                                "players", players != null ? players : 6,
                                "valuePerPlayer", 1000,
                                "distribution", Map.of(
                                        "chip1", 20,
                                        "chip5", 15,
                                        "chip10", 10,
                                        "chip25", 5,
                                        "chip100", 3,
                                        "chip500", 0
                                )
                        )
                )
        ));
    }

    // Optional endpoint for saving custom distributions
    @PostMapping("/chips/save-preset")
    public ResponseEntity<?> saveCustomDistribution(@RequestBody Map<String, Object> distribution) {
        // Simplified implementation - in a real app, this would save to database
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Custom distribution saved successfully",
                "id", "custom-" + System.currentTimeMillis()
        ));
    }
}