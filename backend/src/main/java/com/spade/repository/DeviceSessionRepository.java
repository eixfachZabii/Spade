package com.spade.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {

    Optional<DeviceSessionEntity> findByTokenSha256AndRevokedAtIsNull(String tokenSha256);

    List<DeviceSessionEntity> findByPlayerIdAndRevokedAtIsNullOrderByCreatedAtAscIdAsc(long playerId);

    Optional<DeviceSessionEntity> findByIdAndPlayerIdAndRevokedAtIsNull(long id, long playerId);
}
