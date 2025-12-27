package com.example.LiveHost.repository;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long>, BroadcastRepositoryCustom {
    long countBySellerIdAndStatus(Long sellerId, BroadcastStatus broadcastStatus);

    List<Broadcast> findByStatusAndStartedAtBefore(BroadcastStatus status, LocalDateTime startedAtBefore);
}
