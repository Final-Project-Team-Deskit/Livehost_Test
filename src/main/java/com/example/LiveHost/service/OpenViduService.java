package com.example.LiveHost.service;

import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenViduService {

    private final OpenVidu openVidu;

    // 세션 ID 관리 (BroadcastId -> SessionId)
    // 실제로는 Redis에 저장하거나 DB에 저장하는 것이 좋습니다. (서버 재시작 대비)
    private Map<Long, String> sessionMap = new ConcurrentHashMap<>();

    /**
     * 세션 생성 (또는 기존 세션 반환)
     */
    public String createSession(Long broadcastId) throws OpenViduJavaClientException, OpenViduHttpException {
        // 이미 생성된 세션이 있으면 리턴
        if (sessionMap.containsKey(broadcastId)) {
            return sessionMap.get(broadcastId);
        }

        RecordingProperties recordingProperties = new RecordingProperties.Builder()
                .outputMode(Recording.OutputMode.COMPOSED)
                .build();

        // 세션 속성 설정
        SessionProperties properties = new SessionProperties.Builder()
                .customSessionId("broadcast-" + broadcastId)
                .recordingMode(RecordingMode.MANUAL) // API로 녹화 시작/종료 제어
                .defaultRecordingProperties(recordingProperties)
                .build();

        Session session = openVidu.createSession(properties);
        sessionMap.put(broadcastId, session.getSessionId());

        log.info("OpenVidu 세션 생성: broadcastId={}, sessionId={}", broadcastId, session.getSessionId());
        return session.getSessionId();
    }

    /**
     * 토큰 생성 (Connection 생성)
     */
    public String createToken(Long broadcastId, Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {

        String sessionId = sessionMap.get(broadcastId);
        if (sessionId == null) {
            // 세션이 없으면 생성 시도
            sessionId = createSession(broadcastId);
        }

        Session session = openVidu.getActiveSession(sessionId);
        if (session == null) {
            // 활성 세션이 아니면(닫힌 경우 등) 다시 생성
            sessionId = createSession(broadcastId);
            session = openVidu.getActiveSession(sessionId);
        }

        OpenViduRole role = OpenViduRole.PUBLISHER;
        if (params != null && params.containsKey("role")) {
            role = OpenViduRole.valueOf((String) params.get("role"));
        }

        ConnectionProperties properties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(role)
                .data(params != null ? params.toString() : "")
                .build();

        Connection connection = session.createConnection(properties);
        return connection.getToken();
    }

    public void startRecording(Long broadcastId) throws Exception {
        openVidu.startRecording(sessionMap.get(broadcastId));
    }

    public void stopRecording(Long broadcastId) throws Exception {
        String sessionId = sessionMap.get(broadcastId);
        if (sessionId != null) openVidu.stopRecording(sessionId); // 녹화 중지 -> Webhook 발생
    }

    /**
     * 세션 종료 (방송 종료 시)
     */
    public void closeSession(Long broadcastId) {
        String sessionId = sessionMap.remove(broadcastId);
        if (sessionId != null) {
            try {
                Session session = openVidu.getActiveSession(sessionId);
                if (session != null) {
                    session.close(); // 세션 강제 종료 (모든 연결 끊김)
                    log.info("OpenVidu 세션 종료: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("세션 종료 중 오류: {}", e.getMessage());
            }
        }
    }

    /**
     * 특정 사용자 강제 퇴장 (Force Disconnect)
     */
    public void forceDisconnect(Long broadcastId, String connectionId) {
        String sessionId = sessionMap.get(broadcastId);
        if (sessionId == null) return;

        try {
            Session session = openVidu.getActiveSession(sessionId);
            if (session != null) {
                // connectionId로 연결 종료 시킴
                session.forceDisconnect(connectionId);
                log.info("Force disconnected connection: {}", connectionId);
            }
        } catch (Exception e) {
            log.error("Failed to force disconnect: {}", e.getMessage());
            // 이미 나간 유저일 수 있으므로 예외 무시하거나 로그만 남김
        }
    }
}