package com.example.LiveHost.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinEventMessage {

    private String type;       // "PIN_CHANGE"
    private Long broadcastId;
    private Long productId;    // 핀 된 상품 ID
    private String name;       // 상품명 (화면에 바로 띄워주기 위함)
    private String imageUrl;   // 상품 이미지
    private Integer price;     // 가격
    private boolean isSoldOut; // 품절 여부 확인

    // Socket 전송 시점의 타임스탬프
    private long timestamp;
}
