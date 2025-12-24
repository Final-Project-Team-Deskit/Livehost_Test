package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastLayout;
import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.entity.Broadcast;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BroadcastResponse {

    private Long broadcastId;
    private Long sellerId;
    private String title;
    private String notice;
    private BroadcastStatus status;
    private BroadcastLayout layout;
    private String categoryName; // ID 대신 이름 변환

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    private String thumbnailUrl;
    private String waitScreenUrl;

    // Host에게는 스트림 키 노출 필요 (OpenVidu Session ID)
    private String streamKey;

    // 통계 (조회 시점 기준)
    private Integer totalViews;
    private Integer totalLikes;

    // 연관 데이터 리스트
    private List<BroadcastProductResponse> products;
    private List<QcardResponse> qcards;

    public static BroadcastResponse fromEntity(Broadcast broadcast,
                                               String categoryName,
                                               Integer totalViews,
                                               Integer totalLikes,
                                               List<BroadcastProductResponse> products,
                                               List<QcardResponse> qcards) {
        return BroadcastResponse.builder()
                .broadcastId(broadcast.getBroadcastId())
                .sellerId(broadcast.getSellerId())
                .title(broadcast.getBroadcastTitle())
                .notice(broadcast.getBroadcastNotice())
                .status(broadcast.getStatus())
                .layout(broadcast.getBroadcastLayout())
                .categoryName(categoryName)
                .scheduledAt(broadcast.getScheduledAt())
                .startedAt(broadcast.getStartedAt())
                .thumbnailUrl(broadcast.getBroadcastThumbUrl())
                .waitScreenUrl(broadcast.getBroadcastWaitUrl())
                .streamKey(broadcast.getStreamKey())
                // 통계는 Entity에 없으면 0으로 처리 or 별도 조회
                .totalViews(totalViews != null ? totalViews : 0)
                .totalLikes(totalLikes != null ? totalLikes : 0)
                .products(products)
                .qcards(qcards)
                .build();
    }
}
