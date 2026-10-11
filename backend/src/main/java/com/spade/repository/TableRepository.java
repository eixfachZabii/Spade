package com.spade.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<TableEntity, Long> {

    List<TableEntity> findByClosedAtIsNullOrderByOpenedAtAsc();

    boolean existsByHostIdAndClosedAtIsNull(long hostId);
}
