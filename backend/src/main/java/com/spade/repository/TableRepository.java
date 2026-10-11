package com.spade.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableRepository extends JpaRepository<TableEntity, Long> {

    List<TableEntity> findByClosedAtIsNullOrderByOpenedAtAsc();

    boolean existsByHostIdAndClosedAtIsNull(long hostId);

    /**
     * The table row, locked until the transaction ends (select ... for update). Every write that changes who is
     * seated or what a seat holds takes it first, so one table's writes happen one at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TableEntity t where t.id = :id")
    Optional<TableEntity> findForUpdateById(@Param("id") long id);
}
