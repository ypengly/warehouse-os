package com.warehouseos.controller;

import com.warehouseos.dto.auth.*;
import com.warehouseos.exception.ApiException;
import com.warehouseos.exception.ErrorCode;
import com.warehouseos.security.AppUserPrincipal;
import com.warehouseos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new STAFF account",
            security = @SecurityRequirement(name = ""))
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Exchange credentials for an access and refresh token",
            security = @SecurityRequirement(name = ""))
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Rotate a refresh token for a new token pair",
            security = @SecurityRequirement(name = ""))
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Revoke every refresh token for the current user")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AppUserPrincipal principal) {
        authService.logout(requirePrincipal(principal).getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Return the authenticated user's profile and roles")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.currentUser(requirePrincipal(principal).getUserId());
    }

    private AppUserPrincipal requirePrincipal(AppUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, "Authentication is required");
        }
        return principal;
    }
}
