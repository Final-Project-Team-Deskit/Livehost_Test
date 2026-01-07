package com.example.LiveHost.dto.response;

import com.example.LiveHost.common.enums.UploadType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadConstraintResponse {
    private UploadType type;
    private long maxSizeBytes;
    private double widthRatio;
    private double heightRatio;
    private double targetRatio;
}
