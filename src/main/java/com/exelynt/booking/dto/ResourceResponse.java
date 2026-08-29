package com.exelynt.booking.dto;

import java.math.BigDecimal;

public record ResourceResponse(
        Long id,
        String name,
        String type,
        String description,
        BigDecimal pricePerHour,
        boolean available
) {
}
