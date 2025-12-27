package com.example.LiveHost.controller.member;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.OpenViduRecordingWebhook;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("member/api")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    // [Day 7] OpenVidu Webhook 수신
    @PostMapping("/webhook/openvidu")
    public ResponseEntity<Void> handleWebhook(@RequestBody OpenViduRecordingWebhook payload) {
        if ("recordingStatusChanged".equals(payload.getEvent()) && "ready".equals(payload.getStatus())) {
            broadcastService.processVod(payload);
        }
        return ResponseEntity.ok().build();
    }

    // [Day 7] 좋아요 누르기
    @PostMapping("/broadcasts/{broadcastId}/like")
    public ResponseEntity<ApiResult<Void>> likeBroadcast(@PathVariable Long broadcastId) {
        broadcastService.likeBroadcast(broadcastId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}