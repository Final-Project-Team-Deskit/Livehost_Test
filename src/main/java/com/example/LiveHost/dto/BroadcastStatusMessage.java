package com.example.LiveHost.dto;

import com.example.LiveHost.common.enums.BroadcastStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastStatusMessage {

    private String type; // "STATUS_CHANGE"
    private Long broadcastId;
    private BroadcastStatus status; // ENDED, STOPPED
    private String message; // "방송이 종료되었습니다." 등 안내 문구
}