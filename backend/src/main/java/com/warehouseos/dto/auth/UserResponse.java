package com.warehouseos.dto.auth;

import com.warehouseos.entity.User;
import com.warehouseos.entity.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** Password hash is structurally absent, not merely ignored. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        UserStatus status,
        Set<String> roles,
        Long warehouseId,
        String warehouseName,
        Instant lastLoginAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                user.getWarehouse() != null ? user.getWarehouse().getId() : null,
                user.getWarehouse() != null ? user.getWarehouse().getName() : null,
                user.getLastLoginAt());
    }
}
