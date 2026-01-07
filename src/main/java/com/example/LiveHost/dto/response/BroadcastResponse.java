package com.example.LiveHost.dto.response;

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

    // [수정] 판매자 정보 확장 (UI 표시용)
    private Long sellerId;
    private String sellerName;        // 추가: 판매자 이름/상호명
    private String sellerProfileUrl;  // 추가: 판매자 프로필 이미지

    private String title;
    private String notice;
    private BroadcastStatus status;
    private BroadcastLayout layout;
    private String categoryName; // ID 대신 이름 변환
    private Long categoryId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    private String thumbnailUrl;
    private String waitScreenUrl;

    // Host에게는 스트림 키 노출 필요 (OpenVidu Session ID)
    private String streamKey;
    private String vodUrl;    // Viewer용 VOD 재생 URL

    // 통계 (조회 시점 기준)
    private Integer totalViews;
    private Integer totalLikes;
    private Integer totalReports;

    // 연관 데이터 리스트
    private List<BroadcastProductResponse> products;
    private List<QcardResponse> qcards;

    // [Helper] Factory Method
    public static BroadcastResponse fromEntity(Broadcast broadcast,
                                               String categoryName,
                                               Integer totalViews,
                                               Integer totalLikes,
                                               Integer totalReports,
                                               List<BroadcastProductResponse> products,
                                               List<QcardResponse> qcards,
                                               String vodUrl) { // vodUrl 파라미터 추가
        return BroadcastResponse.builder()
                .broadcastId(broadcast.getBroadcastId())
                // 판매자 정보 매핑
                .sellerId(broadcast.getSeller().getSellerId())
                .sellerName(broadcast.getSeller().getName())     // Entity에서 조회
                .sellerProfileUrl(broadcast.getSeller().getProfile()) // Entity에서 조회 (필드명 확인 필요)

                .title(broadcast.getBroadcastTitle())
                .notice(broadcast.getBroadcastNotice())
                .status(broadcast.getStatus())
                .layout(broadcast.getBroadcastLayout())
                .categoryName(categoryName)
                .categoryId(broadcast.getTagCategory().getTagCategoryId())

                .scheduledAt(broadcast.getScheduledAt())
                .startedAt(broadcast.getStartedAt())
                .thumbnailUrl(broadcast.getBroadcastThumbUrl())
                .waitScreenUrl(broadcast.getBroadcastWaitUrl())

                .streamKey(broadcast.getStreamKey()) // 라이브 시청용 (Session ID)
                .vodUrl(vodUrl)                      // VOD 시청용

                .totalViews(totalViews != null ? totalViews : 0)
                .totalLikes(totalLikes != null ? totalLikes : 0)
                .totalReports(totalReports != null ? totalReports : 0)

                .products(products)
                .qcards(qcards)
                .build();
    }
}
