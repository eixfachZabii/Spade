// src/main/java/com/pokerapp/service/FriendService.java
package com.spadeboot.service;

import com.spadeboot.api.dto.response.FriendshipDto;
import com.spadeboot.domain.user.Friendship;
import com.spadeboot.domain.user.FriendshipStatus;
import com.spadeboot.domain.user.User;
import com.spadeboot.exception.NotFoundException;
import com.spadeboot.repository.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserService userService;

    /**
     * Send a friend request from the current user to another user by username
     * @param username The username of the user to send a request to
     * @return The created friendship record
     */
    @Transactional
    public FriendshipDto sendFriendRequest(String username) {
        User currentUser = userService.getCurrentUser();
        User friend = userService.getUserByUsername(username);

        // Check if they're the same user
        if (currentUser.getUsername().equals(username)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        // Check if there's already a friendship
        Optional<Friendship> existingFriendship = friendshipRepository.findFriendship(currentUser, friend);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();

            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalStateException("You are already friends with this user");
            } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
                if (friendship.getRequester().getId().equals(currentUser.getId())) {
                    throw new IllegalStateException("You have already sent a friend request to this user");
                } else {
                    // The other user has sent a request to the current user, accept it
                    friendship.setStatus(FriendshipStatus.ACCEPTED);
                    friendship.setUpdatedAt(LocalDateTime.now());
                    Friendship savedFriendship = friendshipRepository.save(friendship);
                    return convertToFriendshipDto(savedFriendship);
                }
            } else if (friendship.getStatus() == FriendshipStatus.DECLINED) {
                // If previously declined, allow a new request
                friendship.setStatus(FriendshipStatus.PENDING);
                friendship.setRequester(currentUser);
                friendship.setAddressee(friend);
                friendship.setUpdatedAt(LocalDateTime.now());
                Friendship savedFriendship = friendshipRepository.save(friendship);
                return convertToFriendshipDto(savedFriendship);
            }
        }

        // Create a new friendship request
        Friendship friendship = new Friendship();
        friendship.setRequester(currentUser);
        friendship.setAddressee(friend);
        Friendship savedFriendship = friendshipRepository.save(friendship);

        return convertToFriendshipDto(savedFriendship);
    }

    /**
     * Accept a friend request
     * @param requestId The ID of the friendship record
     * @return The updated friendship record
     */
    @Transactional
    public FriendshipDto acceptFriendRequest(Long requestId) {
        User currentUser = userService.getCurrentUser();

        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        // Verify that the current user is the addressee
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You cannot accept this request as it was not sent to you");
        }

        // Verify the request is pending
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("This request has already been processed");
        }

        // Accept the request
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setUpdatedAt(LocalDateTime.now());
        Friendship savedFriendship = friendshipRepository.save(friendship);

        return convertToFriendshipDto(savedFriendship);
    }

    /**
     * Decline a friend request
     * @param requestId The ID of the friendship record
     * @return The updated friendship record
     */
    @Transactional
    public FriendshipDto declineFriendRequest(Long requestId) {
        User currentUser = userService.getCurrentUser();

        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        // Verify that the current user is the addressee
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You cannot decline this request as it was not sent to you");
        }

        // Verify the request is pending
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("This request has already been processed");
        }

        // Decline the request
        friendship.setStatus(FriendshipStatus.DECLINED);
        friendship.setUpdatedAt(LocalDateTime.now());
        Friendship savedFriendship = friendshipRepository.save(friendship);

        return convertToFriendshipDto(savedFriendship);
    }

    /**
     * Remove a friend by username
     * @param username The username of the friend to remove
     * @return True if successfully removed
     */
    @Transactional
    public boolean removeFriend(String username) {
        User currentUser = userService.getCurrentUser();
        User friend = userService.getUserByUsername(username);

        Optional<Friendship> existingFriendship = friendshipRepository.findFriendship(currentUser, friend);
        if (existingFriendship.isEmpty() || existingFriendship.get().getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("You are not friends with this user");
        }

        friendshipRepository.delete(existingFriendship.get());
        return true;
    }

    /**
     * Get all pending friend requests received by the current user
     * @return List of pending friend requests
     */
    public List<FriendshipDto> getPendingFriendRequests() {
        User currentUser = userService.getCurrentUser();
        List<Friendship> pendingRequests = friendshipRepository.findPendingRequestsReceived(
                currentUser, FriendshipStatus.PENDING);

        return pendingRequests.stream()
                .map(this::convertToFriendshipDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all sent friend requests by the current user
     * @return List of sent friend requests
     */
    public List<FriendshipDto> getSentFriendRequests() {
        User currentUser = userService.getCurrentUser();
        List<Friendship> sentRequests = friendshipRepository.findPendingRequestsSent(
                currentUser, FriendshipStatus.PENDING);

        return sentRequests.stream()
                .map(this::convertToFriendshipDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all friends of the current user
     * @return List of accepted friendships
     */
    public List<FriendshipDto> getFriends() {
        User currentUser = userService.getCurrentUser();
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(
                currentUser, FriendshipStatus.ACCEPTED);

        return friendships.stream()
                .map(this::convertToFriendshipDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if the current user is friends with another user by username
     * @param username The username to check friendship with
     * @return True if they are friends
     */
    public boolean areFriendsByUsername(String username) {
        User currentUser = userService.getCurrentUser();
        User friend = userService.getUserByUsername(username);

        Optional<Friendship> friendship = friendshipRepository.findFriendship(currentUser, friend);
        return friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.ACCEPTED;
    }

    /**
     * Get a specific friendship
     * @param friendshipId The friendship ID
     * @return The friendship DTO
     */
    public FriendshipDto getFriendship(Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new NotFoundException("Friendship not found"));
        return convertToFriendshipDto(friendship);
    }

    /**
     * Convert a friendship entity to DTO
     * @param friendship The friendship entity
     * @return FriendshipDto
     */
    private FriendshipDto convertToFriendshipDto(Friendship friendship) {
        FriendshipDto dto = new FriendshipDto();
        dto.setId(friendship.getId());
        dto.setRequesterId(friendship.getRequester().getId());
        dto.setRequesterUsername(friendship.getRequester().getUsername());
        dto.setAddresseeId(friendship.getAddressee().getId());
        dto.setAddresseeUsername(friendship.getAddressee().getUsername());
        dto.setStatus(friendship.getStatus());
        dto.setCreatedAt(friendship.getCreatedAt());
        dto.setUpdatedAt(friendship.getUpdatedAt());
        return dto;
    }
}