package com.example.LiveHost.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    // 방송별 접속자 관리 (BroadcastId -> [UserId -> Emitter])
    // 간단하게 구현하기 위해 전체 Emitter를 관리하거나, 방송 ID를 키로 사용하는 Map 구조 추천
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long broadcastId, String userId) {
        // 타임아웃 10분 설정
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        String key = broadcastId + "_" + userId;

        emitters.put(key, emitter);

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError((e) -> emitters.remove(key));

        // 연결 성공 시 더미 데이터 전송 (503 에러 방지)
        sendToClient(emitter, key, "connect", "Connected!");

        return emitter;
    }

    /**
     * [1] 범용 알림 전송 (가장 기본이 되는 메서드)
     * @param broadcastId 방송 ID
     * @param eventName   이벤트 이름 (예: "BROADCAST_UPDATED", "SANCTION_UPDATED")
     * @param data        전송할 데이터
     */
    public void notifyBroadcastUpdate(Long broadcastId, String eventName, Object data) {
        emitters.forEach((key, emitter) -> {
            if (key.startsWith(broadcastId + "_")) { // 해당 방송 시청자에게만 전송
                sendToClient(emitter, key, eventName, data);
            }
        });
    }

    /**
     * [2] 편의 메서드: 이벤트 이름만 지정 (데이터는 기본값 "update")
     * 예: notifyBroadcastUpdate(1L, "BROADCAST_ENDED");
     */
    public void notifyBroadcastUpdate(Long broadcastId, String eventName) {
        notifyBroadcastUpdate(broadcastId, eventName, "update");
    }

    /**
     * [3] 편의 메서드: 방송 정보 변경 알림 (기본값 사용)
     * 예: notifyBroadcastUpdate(1L); -> "BROADCAST_UPDATED", "info_changed" 전송
     */
    public void notifyBroadcastUpdate(Long broadcastId) {
        notifyBroadcastUpdate(broadcastId, "BROADCAST_UPDATED", "info_changed");
    }

    /**
     * [신규] 특정 유저 1명에게만 알림 전송 (제재 알림용)
     * Key 생성 규칙(broadcastId_userId)을 이용하여 Map에서 바로 조회
     */
    public void notifyTargetUser(Long broadcastId, Long userId, String eventName, Object data) {
        String key = broadcastId + "_" + userId; // Key 조합
        SseEmitter emitter = emitters.get(key);

        if (emitter != null) {
            sendToClient(emitter, key, eventName, data);
        } else {
            log.warn("Target user not found or disconnected: key={}", key);
        }
    }

    private void sendToClient(SseEmitter emitter, String id, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().id(id).name(name).data(data));
        } catch (IOException e) {
            emitters.remove(id);
        }
    }
}
