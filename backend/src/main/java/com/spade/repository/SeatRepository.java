package com.spade.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    List<SeatEntity> findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(long tableId);

    Optional<SeatEntity> findByPlayerIdAndLeftAtIsNull(long playerId);

    Optional<SeatEntity> findByTableIdAndPlayerIdAndLeftAtIsNull(long tableId, long playerId);
}
