package com.example.LiveHost.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AvailableSlotResponse {
    private LocalDateTime startAt;
    private int remainingSlots;
}
