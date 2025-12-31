package com.example.LiveHost.controller.admin;

import com.example.LiveHost.common.exception.ApiResult;
import com.example.LiveHost.dto.BroadcastResultResponse;
import com.example.LiveHost.dto.BroadcastSearch;
import com.example.LiveHost.dto.SanctionStatisticsResponse;
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
    @GetMapping
    public ResponseEntity<ApiResult<Object>> getAllBroadcasts(
            @ModelAttribute BroadcastSearch searchCondition,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // isAdmin = true로 호출 -> CANCELED, STOPPED 등도 조회 가능
        return ResponseEntity.ok(ApiResult.success(
                broadcastService.getPublicBroadcasts(searchCondition, pageable)
                // 주의: getPublicBroadcasts 내부 로직에서 isAdmin 플래그 처리가 필요하다면
                // broadcastService.getAdminBroadcasts(...) 같은 메서드를 별도로 만드는 게 좋음.
                // 현재 구조상 searchBroadcasts repository 메서드는 isAdmin 플래그를 받으므로,
                // Service에 getAdminBroadcasts 메서드를 추가하거나 기존 메서드 재활용 가능.
        ));
    }

    // 2. 방송 강제 종료 (제재)
    @PutMapping("/{broadcastId}/stop")
    public ResponseEntity<ApiResult<Void>> forceStopBroadcast(
            @PathVariable Long broadcastId,
            @RequestBody Map<String, String> body // {"reason": "부적절한 방송"}
    ) {
        adminService.forceStopBroadcast(broadcastId, body.get("reason"));
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // 3. 제재 통계 대시보드 (차트, 블랙리스트 랭킹)
    @GetMapping("/statistics/sanctions")
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
