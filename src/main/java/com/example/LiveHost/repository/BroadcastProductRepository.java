package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.BroadcastProduct;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface BroadcastProductRepository extends JpaRepository<BroadcastProduct, Long> {
    void deleteByBroadcast(Broadcast broadcast);

    @Modifying
    @Query("UPDATE BroadcastProduct bp SET bp.isPinned = false WHERE bp.broadcast.broadcastId = :broadcastId")
    void resetPinByBroadcastId(@Param("broadcastId") Long broadcastId);

    // [수정] Product 객체 기준 조회
    @Query("SELECT bp FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.product.productId = :pid")
    Optional<BroadcastProduct> findByBroadcastIdAndProductId(@Param("bid") Long bid, @Param("pid") Long pid);

    // [수정] 재고 조회 (bpQuantity)
    @Query("SELECT bp.bpQuantity FROM BroadcastProduct bp WHERE bp.broadcast.broadcastId = :bid AND bp.product.productId = :pid")
    Integer findStock(@Param("bid") Long bid, @Param("pid") Long pid);

    // [New] 실시간 재고 Polling용 (N+1 문제 해결)
    // Product 정보까지 한 번에 가져옴 (이미지, 이름 등)
    @Query("SELECT bp FROM BroadcastProduct bp " +
            "JOIN FETCH bp.product p " +
            "LEFT JOIN FETCH p.images " + // 이미지까지 필요하다면 Fetch Join
            "WHERE bp.broadcast.broadcastId = :broadcastId " +
            "ORDER BY bp.displayOrder ASC")
    List<BroadcastProduct> findAllWithProductByBroadcastId(@Param("broadcastId") Long broadcastId);
}
