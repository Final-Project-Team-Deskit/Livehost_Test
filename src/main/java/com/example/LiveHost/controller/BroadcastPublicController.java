package com.example.LiveHost.controller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.*;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api") // Public 경로는 /api 로 시작
@RequiredArgsConstructor
public class BroadcastPublicController {

    private final BroadcastService broadcastService;

    // 1. 방송 목록 조회 (검색/필터링)
    @GetMapping("/broadcasts")
    public ResponseEntity<ApiResult<Object>> getBroadcasts(
            @ModelAttribute BroadcastSearch searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        // Public은 sellerId = null
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getPublicBroadcasts(searchCondition, pageable)
        ));
    }

    // 2. 방송 상세 조회 (시청 화면 진입 시)
    @GetMapping("/broadcasts/{broadcastId}")
    public ResponseEntity<ApiResult<BroadcastResponse>> getBroadcastDetail(
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getPublicBroadcastDetail(broadcastId)
        ));
    }

    // 3. 방송 입장 (토큰 발급)
    @PostMapping("/broadcasts/{broadcastId}/join")
    public ResponseEntity<ApiResult<String>> joinBroadcast(
            @PathVariable Long broadcastId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId
    ) {
        String token = broadcastService.joinBroadcast(broadcastId, viewerId);
        return ResponseEntity.ok(ApiResult.success(token));
    }

    // 4. 실시간 통계 조회 (Polling용 - 시청자수, 좋아요 등)
    @GetMapping("/broadcasts/{broadcastId}/stats")
    public ResponseEntity<ApiResult<BroadcastStatsResponse>> getBroadcastStats(
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastStats(broadcastId)
        ));
    }

    // =====================================================================
    // [Webhook] OpenVidu 서버가 호출하는 엔드포인트 (인증 제외 필수)
    // SecurityConfig에서 "/api/webhook/**"는 permitAll() 설정되어야 함
    // =====================================================================
    @PostMapping("/webhook/openvidu")
    public ResponseEntity<Void> handleWebhook(@RequestBody OpenViduRecordingWebhook payload) {
        // 녹화가 완료(ready)되었을 때만 처리
        if ("recordingStatusChanged".equals(payload.getEvent()) && "ready".equals(payload.getStatus())) {
            broadcastService.processVod(payload);
        }
        return ResponseEntity.ok().build();
    }
}