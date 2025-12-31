package com.example.LiveHost.repository;

import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.entity.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

    // [퇴장 처리용] 최근 입장 기록 조회
    // updatedAt == createdAt 인 경우(아직 퇴장처리 안됨)를 우선 조회
    @Query("SELECT v FROM ViewHistory v WHERE v.broadcast = :broadcast AND v.viewerId = :viewerId AND v.updatedAt = v.createdAt ORDER BY v.createdAt DESC LIMIT 1")
    Optional<ViewHistory> findActiveHistory(@Param("broadcast") Broadcast broadcast, @Param("viewerId") String viewerId);

    // [평균 시청 시간] (updatedAt - createdAt)의 평균
    // JPQL에서는 SECOND 단위를 인식 못하므로 네이티브 쿼리 사용 필수
    @Query(value = "SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, v.created_at, v.updated_at)), 0) " +
            "FROM view_history v " +
            "WHERE v.broadcast_id = :broadcastId",
            nativeQuery = true)
    Double getAverageWatchTime(@Param("broadcastId") Long broadcastId);

    // [로그 삭제] 오래된 데이터 파기
    @Modifying
    @Query("DELETE FROM ViewHistory v WHERE v.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
