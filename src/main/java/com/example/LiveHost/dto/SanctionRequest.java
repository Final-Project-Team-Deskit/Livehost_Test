package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.SanctionType; // ENUM: MUTE(채팅금지), KICK(강퇴)
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SanctionRequest {

    @NotNull(message = "대상 회원 ID는 필수입니다.")
    private Long targetMemberId; // 제재 당할 사람

    @NotNull(message = "제재 유형은 필수입니다.")
    private SanctionType type; // MUTE, KICK

    private String reason; // 제재 사유 (선택 사항)
}