package com.mcart.product_indexer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@TestConfiguration
public class IntegrationTestJwtConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("sub", "integration-test")
                .claim("roles", List.of("ADMIN"))
                .build();
    }
}
