package com.example.LiveHost.controller.seller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.*;
import com.example.LiveHost.service.BroadcastService;
import com.example.LiveHost.service.RedisService;
import com.example.LiveHost.service.SanctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("seller/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;
    private final RedisService redisService;
    private final SanctionService sanctionService;

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

    // 내 상품 목록 조회
    @GetMapping("/my-products")
    public ResponseEntity<ApiResult<List<ProductSelectResponse>>> getMyProducts(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResult.success(broadcastService.getSellerProducts(sellerId, keyword)));
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

    // [Day 5] 방송 목록 조회 API
    @GetMapping
    public ResponseEntity<ApiResult<Object>> getBroadcasts(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @ModelAttribute BroadcastSearch condition,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.success(broadcastService.getBroadcastList(sellerId, condition, pageable)));
    }

    // 2. [실시간] 통계 조회 (Polling용 - 3초마다 호출)
    // 상품 정보 등 무거운 데이터 제외하고 숫자만 리턴 -> 서버 부하 감소
    @GetMapping("/{broadcastId}/stats")
    public ResponseEntity<ApiResult<Map<String, Integer>>> getRealtimeStats(@PathVariable Long broadcastId) {
        Integer views = redisService.getStatOrZero(redisService.getViewKey(broadcastId));
        Integer likes = redisService.getStatOrZero(redisService.getLikeKey(broadcastId));
        Integer sanctions = redisService.getStatOrZero(redisService.getSanctionKey(broadcastId));

        return ResponseEntity.ok(ApiResult.success(Map.of(
                "totalViews", views,
                "totalLikes", likes,
                "totalSanctions", sanctions
        )));
    }

    // [Day 6] 방송 시작 API
    // PUT /seller/api/broadcasts/{broadcastId}/start
    @PutMapping("/{broadcastId}/start")
    public ResponseEntity<ApiResult<String>> startBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId) {
        // 호스트 토큰 반환
        String token = broadcastService.startBroadcast(sellerId, broadcastId);
        return ResponseEntity.ok(ApiResult.success(token));
    }

    // [Day 6] 방송 종료 API
    // PUT /seller/api/broadcasts/{broadcastId}/end
    @PutMapping("/{broadcastId}/end")
    public ResponseEntity<ApiResult<Void>> endBroadcast(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId) {
        broadcastService.endBroadcast(sellerId, broadcastId);
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

    // [Day 7] 상품 핀 설정
    @PutMapping("/{broadcastId}/products/{productId}/pin")
    public ResponseEntity<ApiResult<Void>> pinProduct(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PathVariable Long broadcastId,
            @PathVariable Long productId) {
        broadcastService.pinProduct(sellerId, broadcastId, productId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

//    // [Day 7] 방송 결과 조회
//    @GetMapping("/{broadcastId}/result")
//    public ResponseEntity<ApiResult<BroadcastResultResponse>> getBroadcastResult(
//            @RequestHeader("X-Seller-Id") Long sellerId,
//            @PathVariable Long broadcastId) {
//        return ResponseEntity.ok(ApiResult.success(broadcastResultService.getBroadcastResult(sellerId, broadcastId)));
//    }
//
//    // [Day 7] VOD 상태 변경
//    @PatchMapping("/{broadcastId}/vod/status")
//    public ResponseEntity<ApiResult<Void>> updateVodStatus(
//            @RequestHeader("X-Seller-Id") Long sellerId,
//            @PathVariable Long broadcastId,
//            @RequestBody VodStatusUpdateRequest request) {
//        vodService.updateVodStatus(sellerId, broadcastId, request);
//        return ResponseEntity.ok(ApiResult.success(null));
//    }
}
