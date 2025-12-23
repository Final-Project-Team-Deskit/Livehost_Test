package com.example.LiveHost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMetricMessage {

    private String type; // "VIEW_COUNT", "LIKE_COUNT"
    private Long broadcastId;

    private Integer totalViews; // 현재 누적 시청자 수
    private Integer currentViewers; // 현재 접속자 수 (Concurrent)
    private Integer totalLikes; // 현재 좋아요 수
}