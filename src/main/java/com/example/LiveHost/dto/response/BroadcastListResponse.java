package com.example.LiveHost.dto.response;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.enums.VodStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastListResponse {
    // --- 1. DB에서 조회하는 기본 정보 (QueryDSL) ---
    private Long broadcastId;
    private String title;
    private String notice;       // 공지사항
    private String sellerName;   // 판매자명
    private String categoryName; // 카테고리명
    private String thumbnailUrl;
    private BroadcastStatus status;

    private LocalDateTime startAt; // 시작/예약 시간
    private LocalDateTime endAt;   // 종료 시간

    // --- 2. 통계 정보 ---
    private int viewerCount;       // (DB) VOD 누적 조회수
    private int liveViewerCount;   // (Redis) 실시간 시청자 수 [New]
    private long reportCount;      // (DB) 신고 수 (Long 타입 권장)
    private BigDecimal totalSales; // (DB) 총 매출
    private int totalLikes;        // (Redis/DB) 좋아요 수

    private boolean isPublic;      // VOD 공개 여부
    private boolean adminLock;     // 관리자 제재 여부

    // --- 3. [New] 판매자 대시보드용 추가 정보 (Service에서 주입) ---
    private List<SimpleProductInfo> products; // 실시간 상품 재고 리스트

    // 내부 클래스: 상품 정보 요약
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class SimpleProductInfo {
        private String name;
        private int stock;
        private boolean isSoldOut;
    }

    // ==================================================================================
    // [QueryDSL Projection용 생성자]
    // Repository에서 이 생성자를 호출하여 기본 데이터를 채웁니다.
    // ==================================================================================
    public BroadcastListResponse(Long broadcastId, String title, String notice,
                                 String sellerName, String categoryName, String thumbnailUrl,
                                 BroadcastStatus status, LocalDateTime scheduledAt,
                                 LocalDateTime startedAt, LocalDateTime endedAt,
                                 Integer totalViews, VodStatus vodStatus, Long reportCount, // Long으로 변경
                                 BigDecimal totalSales, Integer totalLikes) {
        this.broadcastId = broadcastId;
        this.title = title;
        this.notice = notice;
        this.sellerName = sellerName;
        this.categoryName = categoryName;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;

        // 통계 null 처리
        this.reportCount = reportCount != null ? reportCount : 0L;
        this.totalSales = totalSales != null ? totalSales : BigDecimal.ZERO;
        this.totalLikes = totalLikes != null ? totalLikes : 0;
        this.viewerCount = totalViews != null ? totalViews : 0;

        // 시간 매핑 로직
        if (status == BroadcastStatus.RESERVED) {
            this.startAt = scheduledAt;
            this.endAt = (scheduledAt != null) ? scheduledAt.plusMinutes(60) : null;
        } else if (status == BroadcastStatus.ON_AIR || status == BroadcastStatus.READY) {
            this.startAt = (startedAt != null) ? startedAt : LocalDateTime.now();
            this.endAt = null; // 진행 중이라 끝나는 시간 없음
        } else {
            this.startAt = startedAt;
            this.endAt = endedAt;
        }

        // VOD 공개 여부 및 락 설정
        this.isPublic = (vodStatus == VodStatus.PUBLIC);
        this.adminLock = (status == BroadcastStatus.STOPPED);

        // liveViewerCount와 products는 Service의 injectLiveDetails() 메서드에서 채워짐
    }
}