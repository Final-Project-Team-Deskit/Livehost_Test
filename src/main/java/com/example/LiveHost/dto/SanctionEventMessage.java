package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.SanctionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanctionEventMessage {

    private String type; // "SANCTION" (이벤트 타입 구분용)

    private Long broadcastId;
    private Long targetMemberId; // 이 ID를 가진 사람은 나가야 함

    private SanctionType sanctionType; // KICK이면 연결 종료, MUTE면 입력창 비활성화
    private String reason; // "운영 정책 위반으로 강제 퇴장되었습니다." 알림용
}
