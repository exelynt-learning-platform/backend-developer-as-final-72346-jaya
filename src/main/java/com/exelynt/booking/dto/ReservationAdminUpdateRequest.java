package com.exelynt.booking.dto;

import com.exelynt.booking.entity.ReservationStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationAdminUpdateRequest(
        @NotNull Long resourceId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        @NotNull ReservationStatus status
) {
}
