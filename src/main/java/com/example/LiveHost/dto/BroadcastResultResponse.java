package com.example.LiveHost.dto;

import com.example.LiveHost.entity.BroadcastResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BroadcastResultResponse {

    private Long broadcastId;
    private Integer maxViewers;      // 최대 시청자 수 (max_views)
    private Integer totalViewers;    // 누적 시청자 수 (total_views)
    private Integer totalLikes;      // 총 좋아요 수 (total_likes)
    private Integer totalChats;      // 총 채팅 수 (total_chats)
    private BigDecimal totalSales;   // 총 매출액 (total_sales)
    private Integer avgWatchTime;    // 평균 시청 시간 (초 단위)

    private LocalDateTime broadcastDate; // 방송 날짜

    public static BroadcastResultResponse fromEntity(BroadcastResult result) {
        return BroadcastResultResponse.builder()
                .broadcastId(result.getBroadcastId())
                .maxViewers(result.getMaxViews())
                .totalViewers(result.getTotalViews())
                .totalLikes(result.getTotalLikes())
                .totalChats(result.getTotalChats())
                .totalSales(result.getTotalSales())
                .avgWatchTime(result.getAvgWatchTime())
                .broadcastDate(result.getCreatedAt())
                .build();
    }
}
