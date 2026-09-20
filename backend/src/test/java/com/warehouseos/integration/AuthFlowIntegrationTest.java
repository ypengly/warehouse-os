package com.warehouseos.integration;

import com.warehouseos.dto.auth.AuthResponse;
import com.warehouseos.dto.auth.LoginRequest;
import com.warehouseos.dto.auth.RefreshRequest;
import com.warehouseos.dto.auth.RegisterRequest;
import com.warehouseos.exception.ApiException;
import com.warehouseos.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    private RegisterRequest newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new RegisterRequest("user" + suffix, "user" + suffix + "@example.dev",
                "CorrectHorseBattery1", "Test User", null);
    }

    @Test
    void registersAndLogsIn() {
        RegisterRequest request = newUser();
        AuthResponse registered = authService.register(request);

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.user().roles()).containsExactly("STAFF");

        AuthResponse loggedIn = authService.login(
                new LoginRequest(request.username(), request.password()));
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());
    }

    @Test
    void rejectsDuplicateUsername() {
        RegisterRequest request = newUser();
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void rejectsWrongPassword() {
        RegisterRequest request = newUser();
        authService.register(request);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(request.username(), "WrongPassword123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void rotatesRefreshTokenAndRejectsReuse() {
        AuthResponse initial = authService.register(newUser());
        String firstRefresh = initial.refreshToken();

        AuthResponse rotated = authService.refresh(firstRefresh);
        assertThat(rotated.refreshToken()).isNotEqualTo(firstRefresh);

        // Replaying a consumed token must fail.
        assertThatThrownBy(() -> authService.refresh(firstRefresh))
                .isInstanceOf(ApiException.class);
    }
}
