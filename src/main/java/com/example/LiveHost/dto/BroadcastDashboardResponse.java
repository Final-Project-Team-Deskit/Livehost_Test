package com.example.LiveHost.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * [상세 조회] 판매자 대시보드 화면용 최종 데이터
 * 용도: DB 요약 정보 + 주문 서버 상세 정보 + 상품 이미지 등을 모두 조립하여 반환
 */
@Getter
@Builder
public class BroadcastDashboardResponse {

    private Long broadcastId;
    private String title;          // 방송 제목
    private String thumbnailUrl;   // 방송 썸네일

    // --- 1. 전체 요약 (Result 테이블 정보) ---
    private Integer maxViewers;
    private Integer totalViewers;
    private Integer totalLikes;
    private BigDecimal totalRevenue; // 총 순매출

    // --- 2. 상품별 상세 통계 리스트 (핵심) ---
    private List<ProductStatDetail> productStats;

    /**
     * (Inner Class) 대시보드 표(Table)에 들어갈 상품 한 줄 정보
     */
    @Getter
    @Builder
    public static class ProductStatDetail {
        // 상품 기본 정보
        private Long productId;
        private String productName;
        private String imageUrl;
        private int originalPrice;
        private int livePrice;       // 방송 당시 할인가

        // 반응 지표 (Redis)
        private int clickCount;      // 클릭 수 (관심도)

        // 판매 성과 (Order Server 데이터 가공)
        private int quantitySold;    // 판매 수량 (취소 제외)
        private BigDecimal revenue;  // 매출액 (취소 제외)

        // 재고 현황
        private int remainStock;     // 남은 재고
        private boolean isSoldOut;   // 품절 여부
    }
}
