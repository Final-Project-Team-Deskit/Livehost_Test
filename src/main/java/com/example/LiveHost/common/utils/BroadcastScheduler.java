package com.example.LiveHost.common.utils;

import com.example.LiveHost.common.enums.BroadcastStatus;
import com.example.LiveHost.entity.Broadcast;
import com.example.LiveHost.repository.BroadcastRepository;
import com.example.LiveHost.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastScheduler {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastService broadcastService;

    // 1분마다 실행 (30분 체크를 위해 더 자주 실행)
    @Scheduled(cron = "0 * * * * *")
    public void autoClose() {
        // [수정] 현재 시간보다 30분 이전에 시작된(startedAt < now - 30m) 방송 찾기
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Broadcast> targets = broadcastRepository.findByStatusAndStartedAtBefore(BroadcastStatus.ON_AIR, threshold);

        for (Broadcast b : targets) {
            try {
                log.info("방송 시간(30분) 초과로 자동 종료: id={}", b.getBroadcastId());
                broadcastService.endBroadcast(b.getSellerId(), b.getBroadcastId());
            } catch (Exception e) {
                log.error("자동 종료 실패: id={}, msg={}", b.getBroadcastId(), e.getMessage());
            }
        }
    }
}