package com.example.LiveHost.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BroadcastProductRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotNull(message = "라이브 특가는 필수입니다.") // 테이블: bp_price
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer bpPrice;

    @NotNull(message = "방송 판매 수량은 필수입니다.") // 테이블: bp_quantity
    @Min(value = 1, message = "판매 수량은 최소 1개 이상이어야 합니다.")
    private Integer bpQuantity;

    // display_order(순서)는 List의 인덱스로 Service에서 처리
}
