//// src/main/java/com/pokerapp/api/controller/StatisticsController.java
//package com.pokerapp.api.controller;
//
//import com.pokerapp.api.dto.response.StatisticsDto;
//import com.pokerapp.service.StatisticsService;
//import com.pokerapp.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/statistics")
//public class StatisticsController {
//
//    @Autowired
//    private StatisticsService statisticsService;
//
//    @Autowired
//    private UserService userService;
//
//    @GetMapping("/me")
//    public ResponseEntity<StatisticsDto> getMyStatistics() {
//        // Pass the user ID directly - StatisticsService will handle the conversion to Player
//        Long userId = userService.getCurrentUser().getId();
//        return ResponseEntity.ok(statisticsService.getUserStatistics(userId));
//    }
//
//    @GetMapping("/users/{userId}")
//    public ResponseEntity<StatisticsDto> getUserStatistics(@PathVariable Long userId) {
//        // This is already correct, as it passes the user ID to the service
//        return ResponseEntity.ok(statisticsService.getUserStatistics(userId));
//    }
//}
