package com.warehouseos.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                 message = "may only contain letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank @Email @Size(max = 160)
        String email,

        @NotBlank @Size(min = 10, max = 128,
                message = "must be between 10 and 128 characters")
        String password,

        @NotBlank @Size(max = 160)
        String fullName,

        @Size(max = 48)
        String phone
) {}
