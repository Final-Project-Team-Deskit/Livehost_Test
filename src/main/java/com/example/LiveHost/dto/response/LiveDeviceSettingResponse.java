package com.example.LiveHost.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiveDeviceSettingResponse {
    private String cameraId;
    private String microphoneId;
    private String speakerId;
}
