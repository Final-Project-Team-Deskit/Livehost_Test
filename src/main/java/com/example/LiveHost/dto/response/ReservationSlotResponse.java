package com.example.LiveHost.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationSlotResponse {
    private LocalDateTime slotDateTime;
    private int remainingCapacity;
    private boolean selectable;
}
