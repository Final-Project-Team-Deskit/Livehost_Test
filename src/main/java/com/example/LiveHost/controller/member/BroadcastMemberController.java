package com.example.LiveHost.controller.member;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("member/api")
@RequiredArgsConstructor
public class BroadcastMemberController {

    private final BroadcastService broadcastService;

    // 1. 신고하기 (회원 전용)
    @PostMapping("/{broadcastId}/report")
    public ResponseEntity<ApiResult<Void>> reportBroadcast(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long broadcastId
    ) {
        broadcastService.reportBroadcast(broadcastId, memberId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 2. 좋아요 (회원 전용)
    @PostMapping("/{broadcastId}/like")
    public ResponseEntity<ApiResult<Void>> likeBroadcast(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long broadcastId
    ) {
        broadcastService.likeBroadcast(broadcastId, memberId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}