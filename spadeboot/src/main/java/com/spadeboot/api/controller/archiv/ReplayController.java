//// src/main/java/com/pokerapp/api/controller/ReplayController.java
//package com.pokerapp.api.controller;
//
//import com.pokerapp.api.dto.response.ReplayDto;
//import com.pokerapp.service.ReplayService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/replays")
//public class ReplayController {
//
//    @Autowired
//    private ReplayService replayService;
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ReplayDto> getReplay(@PathVariable Long id) {
//        return ResponseEntity.ok(replayService.getReplay(id));
//    }
//
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<ReplayDto>> getReplaysByUser(@PathVariable Long userId) {
//        return ResponseEntity.ok(replayService.getReplaysByUser(userId));
//    }
//
//    @GetMapping("/table/{tableId}")
//    public ResponseEntity<List<ReplayDto>> getReplaysByTable(@PathVariable Long tableId) {
//        return ResponseEntity.ok(replayService.getReplaysByTable(tableId));
//    }
//}