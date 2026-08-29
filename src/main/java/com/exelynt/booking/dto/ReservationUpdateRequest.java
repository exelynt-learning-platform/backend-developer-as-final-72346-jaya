package com.exelynt.booking.dto;

import com.exelynt.booking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationUpdateRequest(
        @NotNull ReservationStatus status
) {
}
