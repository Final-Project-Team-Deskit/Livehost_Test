package com.example.LiveHost.controller.seller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.request.BroadcastCreateRequest;
import com.example.LiveHost.dto.request.BroadcastSearch;
import com.example.LiveHost.dto.request.BroadcastUpdateRequest;
import com.example.LiveHost.dto.request.SanctionRequest;
import com.example.LiveHost.dto.response.BroadcastResponse;
import com.example.LiveHost.dto.response.BroadcastResultResponse;
import com.example.LiveHost.dto.response.ProductSelectResponse;
import com.example.LiveHost.dto.response.StatisticsResponse;
import com.example.LiveHost.service.BroadcastService;
import com.example.LiveHost.service.SanctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("seller/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastSellerController {

    private final BroadcastService broadcastService;
    private final SanctionService sanctionService;

    // 1. 방송 예약 (생성)
    @PostMapping
    public ResponseEntity<ApiResult<Long>> createBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestBody @Valid BroadcastCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.createBroadcast(sellerId, request)
        ));
    }

    // 2. 방송 정보 수정
    @PutMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<Long>> updateBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId,
            @RequestBody @Valid BroadcastUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.updateBroadcast(sellerId, broadcastId, request)
        ));
    }

    // 3. 방송 취소 (삭제)
    @DeleteMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<Void>> cancelBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId
    ) {
        broadcastService.cancelBroadcast(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 내 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<ApiResult<List<ProductSelectResponse>>> getSellerProducts(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getSellerProducts(sellerId, keyword)
        ));
    }

    // 4. 방송 목록 조회 (판매자 대시보드용 - 라이브 상세 정보 포함)
    @GetMapping
    public ResponseEntity<ApiResult<Object>> getSellerBroadcasts(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @ModelAttribute BroadcastSearch searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getSellerBroadcasts(sellerId, searchCondition, pageable)
        ));
    }

    // 5. 방송 상세 조회 (수정 화면 등)
    @GetMapping("/{broadcastId}")
    public ResponseEntity<ApiResult<BroadcastResponse>> getBroadcastDetail(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastDetail(sellerId, broadcastId)
        ));
    }

    // =====================================================================
    // [라이브 제어 API]
    // =====================================================================

    // 6. 방송 시작 (ON_AIR 전환 + 토큰 발급)
    @PostMapping("/{broadcastId}/start")
    public ResponseEntity<ApiResult<String>> startBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId
    ) {
        String token = broadcastService.startBroadcast(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success(token));
    }

    // 7. 방송 종료 (ENDED 전환)
    @PostMapping("/{broadcastId}/end")
    public ResponseEntity<ApiResult<Void>> endBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId
    ) {
        broadcastService.endBroadcast(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 8. 상품 핀 설정 (라이브 중 강조 상품 변경)
    @PostMapping("/{broadcastId}/pin/{productId}")
    public ResponseEntity<ApiResult<Void>> pinProduct(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId,
            @PathVariable Long bpId
    ) {
        broadcastService.pinProduct(sellerId, broadcastId, bpId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // [Day 6] 시청자 제재
    @PostMapping("/{broadcastId}/sanctions")
    public ResponseEntity<ApiResult<Void>> sanctionUser(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId,
            @RequestBody SanctionRequest request) {
        sanctionService.sanctionUser(sellerId, broadcastId, request);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // =====================================================================
    // [통계 API]
    // =====================================================================

    // 9. 통계 대시보드 (매출 차트, 랭킹 등)
    @GetMapping("/statistics")
    public ResponseEntity<ApiResult<StatisticsResponse>> getStatistics(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(defaultValue = "DAILY") String period // DAILY, MONTHLY, YEARLY
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getStatistics(sellerId, period)
        ));
    }

    // 10. 방송 결과 리포트 (개별 방송 상세 통계)
    @GetMapping("/{broadcastId}/report")
    public ResponseEntity<ApiResult<BroadcastResultResponse>> getBroadcastResultReport(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastResult(broadcastId, sellerId, false) // isAdmin=false
        ));
    }

}
