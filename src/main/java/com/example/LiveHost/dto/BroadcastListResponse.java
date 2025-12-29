package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 방송 목록 조회 - 카드형 UI -> 가벼운 정보
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastListResponse {
    private Long broadcastId;
    private String title;
    private Long categoryId;
    private String description;
    private String sellerName;
    private String thumbnailUrl;
    private BroadcastStatus status;
    private LocalDateTime scheduledAt;

    // [추가] 방송 진행 시간/종료 시간 계산용
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    // [추가] VOD 총 조회수 (Live일 때는 Redis값 사용, 여기선 DB값)
    private int totalViews;

    // Live일 때 현재 시청자 수 (Redis에서 별도 주입하거나, 0으로 초기화)
    private int viewCount;

    // [중요] QueryDSL Projections.constructor가 사용할 생성자
    // 파라미터 순서와 타입이 QueryDSL 코드와 100% 일치해야 함
    public BroadcastListResponse(Long broadcastId, String title, String thumbnailUrl,
                                 BroadcastStatus status, LocalDateTime scheduledAt,
                                 LocalDateTime startedAt, LocalDateTime endedAt,
                                 Integer totalViews, int viewCount) {
        this.broadcastId = broadcastId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.totalViews = (totalViews != null) ? totalViews : 0; // null 방지
        this.viewCount = viewCount;
    }
}