package com.example.LiveHost.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LiveDeviceSettingRequest {
    private String cameraId;
    private String microphoneId;
    private String speakerId;
}
