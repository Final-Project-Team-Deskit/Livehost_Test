package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.SanctionType;
import com.example.LiveHost.entity.Sanction;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SanctionResponse {

    private Long sanctionId;
    private Long broadcastId;
    private Long targetMemberId;   // 제재 당한 회원 ID
    private String targetNickname; // (중요) 화면에 표시할 닉네임 (MemberService 연동 필요)

    private SanctionType type;     // KICK(강퇴), MUTE(채팅금지)
    private String reason;         // 제재 사유

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt; // 제재 일시

    public static SanctionResponse fromEntity(Sanction sanction, String nickname) {
        return SanctionResponse.builder()
                .sanctionId(sanction.getSanctionId())
                .broadcastId(sanction.getBroadcast().getBroadcastId())
                .targetMemberId(sanction.getMember().getMemberId())
                .targetNickname(nickname) // 엔티티엔 없고 외부에서 주입
                .type(sanction.getStatus()) // Enum 이름 확인 필요 (SanctionStatus or SanctionType)
                .reason(sanction.getSanctionReason())
                .startedAt(sanction.getCreatedAt())
                .build();
    }
}
