package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastProductStatus;
import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.entity.BroadcastProduct;
import com.example.LiveHost.others.entity.Product;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BroadcastProductResponse {
    private Long bpId;            // 방송상품 ID (PK)
    private Long productId;       // 원본 상품 ID
    private String name;          // 상품명 (Product API 연동 필요)
    private String imageUrl;      // 상품 이미지 (Product API 연동 필요)
    private int originalPrice;    // 원가

    private int bpPrice;        // 라이브 특가 (bp_price)
    private int bpQuantity;     // 판매 수량 (bp_quantity)
    private int displayOrder;     // 노출 순서
    private boolean isPinned;     // 핀 고정 여부 (Y/N)
    private BroadcastProductStatus status;        // 상품 상태 (SELLING, SOLDOUT, DELETED)

    public static BroadcastProductResponse fromEntity(BroadcastProduct bp) {
        Product p = bp.getProduct(); // 연관관계 활용

        return BroadcastProductResponse.builder()
                .bpId(bp.getBpId())
                .productId(p.getProductId())
                .name(p.getProductName())
                .imageUrl(p.getProductThumbUrl()) // Product 엔티티의 편의 메서드 사용
                .originalPrice(p.getPrice())
                .bpPrice(bp.getBpPrice())
                .bpQuantity(bp.getBpQuantity())
                .displayOrder(bp.getDisplayOrder())
                .isPinned(bp.isPinned())
                .status(bp.getStatus())
                .build();
    }
}