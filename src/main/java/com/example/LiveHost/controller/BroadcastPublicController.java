package com.example.LiveHost.controller;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.request.BroadcastSearch;
import com.example.LiveHost.dto.request.OpenViduRecordingWebhook;
import com.example.LiveHost.dto.response.BroadcastProductResponse;
import com.example.LiveHost.dto.response.BroadcastResponse;
import com.example.LiveHost.dto.response.BroadcastStatsResponse;
import com.example.LiveHost.service.BroadcastService;
import com.example.LiveHost.service.SseService;
import com.example.LiveHost.service.VodService;
import com.example.LiveHost.service.VodStreamData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api") // Public 경로는 /api 로 시작
@RequiredArgsConstructor
public class BroadcastPublicController {

    private final BroadcastService broadcastService;
    private final SseService sseService;
    private final VodService vodService;

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

    // 5. [Polling B] 실시간 상품 정보 (DB - 3~5초 간격 권장)
    // 재고, 가격, 핀 상태
    @GetMapping("/broadcasts/{broadcastId}/products")
    public ResponseEntity<ApiResult<List<BroadcastProductResponse>>> getProducts(@PathVariable Long broadcastId) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastProducts(broadcastId)
        ));
    }

    // 6. SSE 구독 연결
    @GetMapping(value = "/broadcasts/{broadcastId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Long broadcastId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId,
            @RequestParam(value = "viewerId", required = false) String viewerIdParam
    ) {
        // 비회원도 구독 가능 (UUID 사용) - 헤더 우선, 없으면 쿼리 파라미터 사용
        String userId = (viewerId != null) ? viewerId : (viewerIdParam != null ? viewerIdParam : "anonymous");
        return sseService.subscribe(broadcastId, userId);
    }

    // 7. VOD 스트리밍 (Range 지원)
    @GetMapping("/broadcasts/{broadcastId}/vod/stream")
    public ResponseEntity<InputStreamResource> streamVod(
            @PathVariable Long broadcastId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {
        try {
            VodStreamData streamData = vodService.getVodStream(broadcastId, rangeHeader);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(streamData.contentType()));
            headers.setContentLength(streamData.contentLength());
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");

            if (rangeHeader != null && !rangeHeader.isBlank()) {
                headers.add(
                        HttpHeaders.CONTENT_RANGE,
                        "bytes " + streamData.rangeStart() + "-" + streamData.rangeEnd() + "/" + streamData.totalLength()
                );
                return new ResponseEntity<>(new InputStreamResource(streamData.stream()), headers, HttpStatus.PARTIAL_CONTENT);
            }
            return new ResponseEntity<>(new InputStreamResource(streamData.stream()), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes */0");
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }
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
