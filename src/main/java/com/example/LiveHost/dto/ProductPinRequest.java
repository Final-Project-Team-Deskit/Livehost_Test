package com.example.LiveHost.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductPinRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    // 만약 '핀 해제' 기능도 있다면 productId가 null일 수 있겠지만,
    // 보통 다른 상품을 누르면 교체되는 방식이므로 필수값으로 둡니다.
}