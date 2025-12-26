package com.example.LiveHost.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class BroadcastAllResponse {
    private BroadcastListResponse liveBroadcast;        // 방송 중 (1개)
    private List<BroadcastListResponse> reservedBroadcasts; // 예약 (5개)
    private List<BroadcastListResponse> vodBroadcasts;      // VOD (5개)
}
