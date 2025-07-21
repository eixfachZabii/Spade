// src/main/java/com/pokerapp/api/controller/FriendController.java
package com.spadeboot.api.controller;

import com.spadeboot.api.dto.request.FriendRequestDto;
import com.spadeboot.api.dto.response.FriendshipDto;
import com.spadeboot.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @PostMapping("/requests")
    public ResponseEntity<FriendshipDto> sendFriendRequest(@Valid @RequestBody FriendRequestDto requestDto) {
        return ResponseEntity.ok(friendService.sendFriendRequest(requestDto.getUsername()));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<FriendshipDto>> getPendingFriendRequests() {
        return ResponseEntity.ok(friendService.getPendingFriendRequests());
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<List<FriendshipDto>> getSentFriendRequests() {
        return ResponseEntity.ok(friendService.getSentFriendRequests());
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendshipDto> acceptFriendRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(friendService.acceptFriendRequest(requestId));
    }

    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<FriendshipDto> declineFriendRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(friendService.declineFriendRequest(requestId));
    }

    @GetMapping
    public ResponseEntity<List<FriendshipDto>> getFriends() {
        return ResponseEntity.ok(friendService.getFriends());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FriendshipDto> getFriendship(@PathVariable Long id) {
        return ResponseEntity.ok(friendService.getFriendship(id));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> removeFriend(@PathVariable String username) {
        boolean removed = friendService.removeFriend(username);
        if (removed) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Friend removed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to remove friend"));
        }
    }

    @GetMapping("/check/{username}")
    public ResponseEntity<?> checkFriendship(@PathVariable String username) {
        boolean areFriends = friendService.areFriendsByUsername(username);
        return ResponseEntity.ok(Map.of("areFriends", areFriends));
    }
}