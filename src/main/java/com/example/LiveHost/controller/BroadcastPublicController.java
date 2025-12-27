package com.example.LiveHost.controller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.OpenViduRecordingWebhook;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BroadcastPublicController {

    private final BroadcastService broadcastService;

    // [Day 7] OpenVidu Webhook 수신
    @PostMapping("/webhook/openvidu")
    public ResponseEntity<Void> handleWebhook(@RequestBody OpenViduRecordingWebhook payload) {
        if ("recordingStatusChanged".equals(payload.getEvent()) && "ready".equals(payload.getStatus())) {
            broadcastService.processVod(payload);
        }
        return ResponseEntity.ok().build();
    }

    // [Day 7] 상품 실시간 재고 조회
    // GET /api/broadcasts/{broadcastId}/products/{productId}/stock
    @GetMapping("/broadcasts/{broadcastId}/products/{productId}/stock")
    public ResponseEntity<ApiResult<Integer>> getProductStock(
            @PathVariable Long broadcastId,
            @PathVariable Long productId) {

        Integer stock = broadcastService.getProductStock(broadcastId, productId);
        return ResponseEntity.ok(ApiResult.success(stock));
    }
}
