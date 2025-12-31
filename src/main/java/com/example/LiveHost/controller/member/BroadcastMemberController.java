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
public class BroadcastMemberController {

    private final BroadcastService broadcastService;

    // [Day 7] 좋아요 누르기
    @PostMapping("/broadcasts/{broadcastId}/like")
    public ResponseEntity<ApiResult<Void>> likeBroadcast(@RequestHeader("X-Member-Id") Long memberId,
                                                         @PathVariable Long broadcastId) {
        broadcastService.likeBroadcast(broadcastId, memberId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 5. 방송 신고 (비회원도 가능, X-Viewer-Id 필수)
    @PostMapping("/broadcasts/{broadcastId}/report")
    public ResponseEntity<ApiResult<Void>> reportBroadcast(
            @PathVariable Long broadcastId,
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        broadcastService.reportBroadcast(broadcastId, memberId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}