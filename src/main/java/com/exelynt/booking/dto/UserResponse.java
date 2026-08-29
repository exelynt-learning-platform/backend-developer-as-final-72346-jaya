package com.exelynt.booking.dto;

import com.exelynt.booking.entity.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
}
