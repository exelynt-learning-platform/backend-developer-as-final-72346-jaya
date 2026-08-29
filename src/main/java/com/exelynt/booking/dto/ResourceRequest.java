package com.exelynt.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ResourceRequest(
        @NotBlank String name,
        @NotBlank String type,
        String description,
        @NotNull @DecimalMin(value = "0.00") BigDecimal pricePerHour,
        Boolean available
) {
}
