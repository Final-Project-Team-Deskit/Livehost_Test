package com.example.LiveHost.service;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.dto.response.SanctionStatisticsResponse;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final SanctionRepository sanctionRepository;
    private final BroadcastRepository broadcastRepository;
    private final OpenViduService openViduService;
    private final RedisService redisService;
    private final SseService sseService;
    private final SanctionService sanctionService;

    @Transactional(readOnly = true)
    public SanctionStatisticsResponse getSanctionStatistics(String period) {
        return SanctionStatisticsResponse.builder()
                .forceStopChart(sanctionRepository.getSellerForceStopChart(period)) // 방송 테이블
                .viewerBanChart(sanctionRepository.getViewerSanctionChart(period)) // 제재 테이블
                .worstSellers(sanctionRepository.getSellerForceStopRanking(period, 5))
                .worstViewers(sanctionRepository.getViewerSanctionRanking(period,5))
                .build();
    }

    @Transactional
    public void forceStopBroadcast(Long broadcastId, String reason) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.forceStopByAdmin(reason); // 상태 변경 (Sanction 테이블 저장 X)

        openViduService.closeSession(broadcastId);
        redisService.deleteBroadcastKeys(broadcastId);
        sseService.notifyBroadcastUpdate(broadcastId, "BROADCAST_STOPPED", reason);
    }

    // =====================================================================
    // [관리자] 방송 예약 취소 (CANCELED)
    // =====================================================================
    @Transactional
    public void cancelBroadcast(Long broadcastId, String reason) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BROADCAST_NOT_FOUND));

        // 이미 시작했거나 종료된 방송은 취소 불가 (강제 종료 사용해야 함)
        if (broadcast.getStatus() != BroadcastStatus.RESERVED && broadcast.getStatus() != BroadcastStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        broadcast.cancelBroadcast(reason); // Status -> CANCELED
    }

    @Transactional
    public void sanctionViewer(Long adminId, Long broadcastId, com.example.LiveHost.dto.request.SanctionRequest request) {
        sanctionService.sanctionUserByAdmin(adminId, broadcastId, request);
    }
}
