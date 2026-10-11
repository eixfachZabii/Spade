package com.spade.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    boolean existsByUsername(String username);

    Optional<PlayerEntity> findByUsername(String username);
}
