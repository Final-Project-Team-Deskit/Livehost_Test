package com.example.LiveHost.controller;

import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*") // 어떤 도메인에서든 애플리케이션에 접근 가능, 추후 수정
@RestController
@RequestMapping("/api")
public class LiveHostController {
    @Value("${OPENVIDU_URL}")
    private String OPENVIDU_URL;

    @Value("${OPENVIDU_SECRET}")
    private String OPENVIDU_SECRET;

    private OpenVidu openvidu;

    @PostConstruct
    public void init() {
        // OpenVidu 객체 초기화 (미디어 서버와 연결 준비)
        this.openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }

    // 1. 방송 방(session) 만들기
    @PostMapping("/sessions")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        SessionProperties properties = SessionProperties.fromJson(params).build();
        Session session = openvidu.createSession(properties);
        return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
    }

    // 연결 생성(입장권 만들기, 토큰 반환)
    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {

        Session session = this.openvidu.getActiveSession(sessionId);
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // [수정] 클라이언트에서 요청한 role 값을 확인 (기본값: SUBSCRIBER)
        OpenViduRole role = OpenViduRole.SUBSCRIBER;
        if (params != null && params.containsKey("role")) {
            String roleParam = (String) params.get("role");
            if ("PUBLISHER".equals(roleParam)) {
                role = OpenViduRole.PUBLISHER;
            }
        }

        // [수정] 연결 속성에 Role 설정
        ConnectionProperties properties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(role) // 설정된 Role 적용
                .data(params != null ? (String) params.get("clientData") : "")
                .build();

        Connection connection = session.createConnection(properties);

        return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);
    }


}
