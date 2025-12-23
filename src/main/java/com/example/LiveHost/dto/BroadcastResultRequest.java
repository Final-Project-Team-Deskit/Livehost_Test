package com.example.LiveHost.dto;

import com.example.LiveHost.entity.BroadcastResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BroadcastResultRequest {

    private Long broadcastId;

    private int maxViewers;      // 최고 시청자 수 (Redis Max값)
    private int totalViewers;    // 누적 접속자 수 (Redis Set size)
    private int totalLikes;      // 누적 좋아요 (Redis Incr)
    private int totalChats;      // 누적 채팅 수 (MongoDB/Redis Count)

    private BigDecimal totalSales; // 주문 서버에서 받아온 총 매출액
    private int avgWatchTime;      // 평균 시청 시간 (초)
    private LocalDateTime startedAt; // 방송 실제 시작 시간
    private LocalDateTime endedAt;   // 방송 실제 종료 시간

    // DTO -> Entity 변환
    public BroadcastResult toEntity() {
        return BroadcastResult.builder()
                .broadcastId(this.broadcastId)
                .maxViews(this.maxViewers)
                .totalViews(this.totalViewers)
                .totalLikes(this.totalLikes)
                .totalChats(this.totalChats)
                .totalSales(this.totalSales)
                .avgWatchTime(this.avgWatchTime)
                // calculatedAt은 Entity의 @CreationTimestamp 등으로 처리하거나 현재시간 주입
                .build();
    }
}
