package com.example.LiveHost.controller.admin;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.BroadcastResultResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import com.example.LiveHost.dto.SanctionStatisticsResponse;
import com.example.LiveHost.dto.StatisticsResponse;
import com.example.LiveHost.service.AdminService;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class BroadcastAdminController {
    private final AdminService adminService;
    private final BroadcastService broadcastService;

    // 1. 관리자용 방송 목록 조회 (모든 상태/판매자 조회 가능)
    // 1. 전체 방송 목록 조회 (모니터링용)
    @GetMapping("/broadcasts")
    public ResponseEntity<ApiResult<Object>> getAllBroadcasts(
            @ModelAttribute BroadcastSearch searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getAdminBroadcasts(searchCondition, pageable)
        ));
    }

    // 2. 방송 강제 종료 (STOPPED)
    @PutMapping("/broadcasts/{broadcastId}/stop")
    public ResponseEntity<ApiResult<Void>> forceStop(
            @PathVariable Long broadcastId,
            @RequestBody Map<String, String> body
    ) {
        adminService.forceStopBroadcast(broadcastId, body.get("reason"));
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 3. 예약 방송 취소 (CANCELED)
    @PutMapping("/broadcasts/{broadcastId}/cancel")
    public ResponseEntity<ApiResult<Void>> cancelBroadcast(
            @PathVariable Long broadcastId
    ) {
        adminService.cancelBroadcast(broadcastId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 4. 전체 플랫폼 통계 (매출/시청자)
    @GetMapping("/statistics")
    public ResponseEntity<ApiResult<StatisticsResponse>> getAdminStatistics(
            @RequestParam(defaultValue = "DAILY") String period
    ) {
        // sellerId = null -> 전체 통계
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getStatistics(null, period)
        ));
    }

    // 5. 제재 현황 통계
    @GetMapping("/sanctions/statistics")
    public ResponseEntity<ApiResult<SanctionStatisticsResponse>> getSanctionStatistics(
            @RequestParam(defaultValue = "DAILY") String period
    ) {
        return ResponseEntity.ok(ApiResult.success(
                adminService.getSanctionStatistics(period)
        ));
    }

    // 4. 개별 방송 결과 리포트 조회 (관리자 권한)
    @GetMapping("/{broadcastId}/report")
    public ResponseEntity<ApiResult<BroadcastResultResponse>> getBroadcastReport(
            @PathVariable Long broadcastId
    ) {
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getBroadcastResult(broadcastId, null, true) // isAdmin=true
        ));
    }
}
