package com.spadeboot.repository;

import com.spadeboot.domain.user.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query("SELECT p FROM Player p WHERE p.user.id = :userId")
    Optional<Player> findByUserId(@Param("userId") Long userId);

    // Determine if a player exists for a given user ID
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Player p WHERE p.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}