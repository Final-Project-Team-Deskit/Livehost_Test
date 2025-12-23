package com.example.LiveHost.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OpenViduTokenResponse {

    private String sessionId; // 방송 방 ID (OpenVidu Session)
    private String token;     // 접속 권한 토큰 (이게 있어야 송출 가능)
    private String role;      // PUBLISHER(판매자) or SUBSCRIBER(시청자)
    private String serverUrl; // OpenVidu 서버 주소 (필요 시)
}
