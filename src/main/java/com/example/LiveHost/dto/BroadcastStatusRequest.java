package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BroadcastStatusRequest {

    @NotNull(message = "변경할 방송 상태는 필수입니다.")
    private BroadcastStatus status; // ON_AIR(시작), ENDED(종료)

    // 방송 중지(강제종료) 시에만 입력되는 사유
    private String stoppedReason;
}
