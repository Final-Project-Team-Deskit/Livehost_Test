package com.example.LiveHost.controller.seller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.BroadcastCreateRequest;
import com.example.LiveHost.dto.BroadcastResponse;
import com.example.LiveHost.dto.BroadcastUpdateRequest;
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

    // [방송 수정] - @HostCheck: AOP가 먼저 가로채서 본인 방송인지 확인합니다.
    // @HostCheck - 다만 테스트 단계에서 아직 회원 권한 쪽과 합쳐지지 않았기에 주석 처리
    @PutMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<Long>> updateBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId,
            @Valid @RequestBody BroadcastUpdateRequest request) {

        Long updatedId = broadcastService.updateBroadcast(sellerId, broadcastId, request);
        return ResponseEntity.ok(ApiResult.success(updatedId));
    }

    // 방송 상세 조회 (수정 화면 진입용)
        // GET /seller/api/broadcasts/{broadcastId}
    @GetMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<BroadcastResponse>> getBroadcastDetail(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId) {
        BroadcastResponse response = broadcastService.getBroadcastDetail(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    // [방송 취소]
    // @HostCheck 적용
    // @HostCheck
    @DeleteMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<String>> cancelBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId) {

        broadcastService.cancelBroadcast(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success("방송 예약이 취소되었습니다."));
    }
}
