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

    // [핵심] 방송 정보가 수정되었을 때 알림 전송
    public void notifyBroadcastUpdate(Long broadcastId) {
        String eventName = "BROADCAST_UPDATED"; // 프론트에서 이 이벤트를 리스닝

        emitters.forEach((key, emitter) -> {
            if (key.startsWith(broadcastId + "_")) { // 해당 방송 시청자에게만 전송
                sendToClient(emitter, key, eventName, "update");
            }
        });
    }

    private void sendToClient(SseEmitter emitter, String id, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().id(id).name(name).data(data));
        } catch (IOException e) {
            emitters.remove(id);
        }
    }
}
