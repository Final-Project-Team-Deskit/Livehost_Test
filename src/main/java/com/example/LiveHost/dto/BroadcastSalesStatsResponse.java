package com.example.LiveHost.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@ToString
public class BroadcastSalesStatsResponse {

    private Long productId;          // 상품 ID

    // --- 판매 실적 ---
    private Integer totalOrderCount; // 총 결제 건수
    private Integer totalQuantity;   // 총 판매 수량
    private BigDecimal totalAmount;  // 총 판매 금액

    // --- 취소/환불 실적 ---
    private Integer cancelCount;     // 취소 건수
    private Integer cancelQuantity;  // 취소 수량
    private BigDecimal cancelAmount; // 취소 금액
}