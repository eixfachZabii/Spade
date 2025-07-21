// src/main/java/com/pokerapp/repository/FriendshipRepository.java
package com.spadeboot.repository;

import com.spadeboot.domain.user.Friendship;
import com.spadeboot.domain.user.FriendshipStatus;
import com.spadeboot.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Find a specific friendship between two users
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :user1 AND f.addressee = :user2) OR " +
            "(f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findFriendship(@Param("user1") User user1, @Param("user2") User user2);

    // Find all pending friend requests received by a user
    @Query("SELECT f FROM Friendship f WHERE f.addressee = :user AND f.status = :status")
    List<Friendship> findPendingRequestsReceived(@Param("user") User user, @Param("status") FriendshipStatus status);

    // Find all pending friend requests sent by a user
    @Query("SELECT f FROM Friendship f WHERE f.requester = :user AND f.status = :status")
    List<Friendship> findPendingRequestsSent(@Param("user") User user, @Param("status") FriendshipStatus status);

    // Find all accepted friendships for a user
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.addressee = :user) AND f.status = :status")
    List<Friendship> findAcceptedFriendships(@Param("user") User user, @Param("status") FriendshipStatus status);
}