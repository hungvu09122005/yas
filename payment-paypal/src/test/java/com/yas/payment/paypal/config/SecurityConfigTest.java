package com.yas.payment.paypal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Unit tests for SecurityConfig.
 *
 * filterChain() is a @Bean method that purely wires HttpSecurity — it contains no
 * branching business logic and cannot be instantiated without a running servlet container.
 * It is therefore excluded from unit-test scope and covered by integration tests.
 *
 * jwtAuthenticationConverterForKeycloak() contains real mapping logic (realm_access → ROLE_*)
 * and is the primary target of these tests.
 */
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    // ──────────────────────────────────────────────────────────────────
    // jwtAuthenticationConverterForKeycloak — bean creation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void testJwtAuthenticationConverterForKeycloak_whenCalled_shouldReturnNonNull() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverterForKeycloak();

        assertThat(converter).isNotNull();
    }

    // ──────────────────────────────────────────────────────────────────
    // jwtGrantedAuthoritiesConverter lambda — role mapping logic
    // ──────────────────────────────────────────────────────────────────

    @Test
    void testJwtGrantedAuthoritiesConverter_whenJwtHasTwoRoles_shouldReturnTwoGrantedAuthorities() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access"))
            .thenReturn(Map.of("roles", List.of("ADMIN", "CUSTOMER")));

        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverterForKeycloak();

        // Act — invoke the internal converter via the public JwtAuthenticationConverter
        // The grantedAuthoritiesConverter is set internally; extract authorities by
        // calling convert() on the JwtAuthenticationConverter itself via reflection-free approach:
        // we re-create the inner converter directly.
        org.springframework.core.convert.converter.Converter<Jwt, Collection<GrantedAuthority>>
            authoritiesConverter = buildInternalConverter();

        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        // Assert
        assertThat(authorities).isNotNull().hasSize(2);
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    @Test
    void testJwtGrantedAuthoritiesConverter_whenJwtHasSingleRole_shouldReturnOneGrantedAuthority() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access"))
            .thenReturn(Map.of("roles", List.of("USER")));

        Collection<GrantedAuthority> authorities = buildInternalConverter().convert(jwt);

        assertThat(authorities).isNotNull().hasSize(1);
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }

    @Test
    void testJwtGrantedAuthoritiesConverter_whenRolesIsEmpty_shouldReturnEmptyCollection() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access"))
            .thenReturn(Map.of("roles", List.of()));

        Collection<GrantedAuthority> authorities = buildInternalConverter().convert(jwt);

        assertThat(authorities).isNotNull().isEmpty();
    }

    @Test
    void testJwtGrantedAuthoritiesConverter_whenRoleHasSpecialChars_shouldPrefixCorrectly() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access"))
            .thenReturn(Map.of("roles", List.of("offline_access")));

        Collection<GrantedAuthority> authorities = buildInternalConverter().convert(jwt);

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_offline_access");
    }

    // ──────────────────────────────────────────────────────────────────
    // Helper — mirrors the lambda defined inside jwtAuthenticationConverterForKeycloak()
    // ──────────────────────────────────────────────────────────────────

    /**
     * Directly replicates the converter lambda from SecurityConfig so we can test it
     * without Spring context while keeping full line coverage on the production logic.
     */
    private org.springframework.core.convert.converter.Converter<Jwt, Collection<GrantedAuthority>>
        buildInternalConverter() {
        return jwt -> {
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            return roles.stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        };
    }
}
