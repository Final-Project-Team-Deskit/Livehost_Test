package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;

import java.time.LocalDateTime;

// 방송 목록 조회 - 카드형 UI -> 가벼운 정보
public class BroadcastListResponse {
    private Long broadcastId;
    private String title;
    private String thumbnailUrl;
    private BroadcastStatus status;
    private LocalDateTime scheduledAt;
    private int viewCount; // 현재 시청자
}