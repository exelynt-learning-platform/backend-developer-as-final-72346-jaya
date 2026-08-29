package com.exelynt.booking.dto;

public record LoginResponse(
        String token,
        String tokenType,
        UserResponse user
) {
}
