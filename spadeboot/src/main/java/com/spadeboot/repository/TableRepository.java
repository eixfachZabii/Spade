// src/main/java/com/pokerapp/repository/TableRepository.java
package com.spadeboot.repository;

import com.spadeboot.domain.game.PokerTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<PokerTable, Long> {

}
