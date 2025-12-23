package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.VodStatus; // PUBLIC, PRIVATE, DELETED
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VodStatusUpdateRequest {

    @NotNull(message = "변경할 VOD 상태는 필수입니다.")
    private VodStatus status;

    // 필요하다면 '비공개 사유' 등을 받을 수도 있음
    // private String privateReason;
}