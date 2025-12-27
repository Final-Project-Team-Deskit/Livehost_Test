package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BroadcastProductRepository extends JpaRepository<BroadcastProduct, Long> {
    void deleteByBroadcast(Broadcast broadcast);

    // [Day 7] 핀 초기화
    @Modifying
    @Query("UPDATE BroadcastProduct bp SET bp.isPinned = false WHERE bp.broadcast.broadcastId = :broadcastId")
    void resetPinByBroadcastId(@Param("broadcastId") Long broadcastId);

    // [Day 7] 특정 상품 조회
    @Query("SELECT bp FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.productId = :pid")
    Optional<BroadcastProduct> findByBroadcastIdAndProductId(@Param("bid") Long bid, @Param("pid") Long pid);

    // [Day 7] 재고 조회
    @Query("SELECT bp.bpQuantity FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.productId = :pid")
    Integer findStock(@Param("bid") Long bid, @Param("pid") Long pid);
}
