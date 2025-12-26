package com.example.LiveHost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSelectResponse {
    private Long productId;
    private String productName;
    private Integer price;      // 정가
    private Integer stockQty;   // 현재 재고
    // private String status;   // 제거됨 (이미 판매 가능한 상품만 조회하므로)
    private String imageUrl;    // 대표 이미지 URL
}
