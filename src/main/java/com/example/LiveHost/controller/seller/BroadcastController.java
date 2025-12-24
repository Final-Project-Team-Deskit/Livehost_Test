package com.example.LiveHost.controller.seller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.BroadcastCreateRequest;
import com.example.LiveHost.service.BroadcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("seller/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    // 방송 생성 API
    // POST /api/v1/broadcasts
    @PostMapping
    public ResponseEntity<ApiResult<Long>> createBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId, // Gateway 등에서 넘어온 판매자 ID
            @Valid @RequestBody BroadcastCreateRequest request) {

        Long broadcastId = broadcastService.createBroadcast(sellerId, request);
        return ResponseEntity.ok(ApiResult.success(broadcastId));
    }
}
