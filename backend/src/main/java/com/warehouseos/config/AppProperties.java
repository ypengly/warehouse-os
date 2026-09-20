package com.warehouseos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed) {

    public record Jwt(String secret, String issuer,
                      Duration accessTokenTtl, Duration refreshTokenTtl) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Seed(boolean enabled) {}
}
