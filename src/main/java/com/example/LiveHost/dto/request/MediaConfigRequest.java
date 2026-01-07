package com.example.LiveHost.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MediaConfigRequest {
    @NotNull
    private String cameraId;

    @NotNull
    private String microphoneId;

    @NotNull
    private Boolean cameraOn;

    @NotNull
    private Boolean microphoneOn;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer volume;
}
