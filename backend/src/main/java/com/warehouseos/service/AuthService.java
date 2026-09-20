package com.warehouseos.service;

import com.warehouseos.config.AppProperties;
import com.warehouseos.dto.auth.*;
import com.warehouseos.entity.*;
import com.warehouseos.exception.*;
import com.warehouseos.repository.*;
import com.warehouseos.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties properties;

    /**
     * Self-registration always produces a STAFF account. Elevating a user to
     * MANAGER or ADMIN is an admin-only operation, so the public endpoint can
     * never be used to mint a privileged account.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        Role staff = roleRepository.findByName(RoleName.STAFF)
                .orElseThrow(() -> new IllegalStateException("STAFF role missing; check migrations"));

        User user = new User();
        user.setUsername(request.username().toLowerCase());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(staff));

        userRepository.save(user);
        log.info("Registered new user id={} username={}", user.getId(), user.getUsername());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.username(), request.password()));
        } catch (AuthenticationException ex) {
            // Do not log the attempted password, and do not distinguish causes.
            log.info("Failed login attempt for '{}'", request.username());
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials");
        }

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .or(() -> userRepository.findByEmailIgnoreCase(request.username()))
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                        "Invalid credentials"));

        user.setLastLoginAt(Instant.now());
        return issueTokens(user);
    }

    /** Refresh-token rotation: the presented token is revoked as it is consumed. */
    @Transactional
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(presentedToken))
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                        "Invalid or expired refresh token"));

        if (!stored.isUsable()) {
            // Reuse of a revoked token suggests theft: drop the whole family.
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                    "Invalid or expired refresh token");
        }

        stored.setRevokedAt(Instant.now());
        User user = userRepository.findById(stored.getUser().getId())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                        "Invalid or expired refresh token"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED, "Account is not active");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        log.debug("Revoked {} refresh token(s) for user {}", revoked, userId);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    // ------------------------------------------------------------------

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = newOpaqueToken();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256(refreshToken));
        entity.setExpiresAt(Instant.now().plus(properties.jwt().refreshTokenTtl()));
        refreshTokenRepository.save(entity);

        return AuthResponse.of(accessToken, refreshToken,
                jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
    }

    private static String newOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
