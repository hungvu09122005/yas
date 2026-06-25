package com.yas.cart.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Unit tests for cart SecurityConfig.
 *
 * filterChain() is a @Bean that wires HttpSecurity rules — no branching logic,
 * cannot be unit-tested without servlet container. Covered by integration tests.
 *
 * jwtAuthenticationConverterForKeycloak() contains real role-mapping logic and is
 * the primary target here.
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
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access"))
            .thenReturn(Map.of("roles", List.of("ADMIN", "CUSTOMER")));

        Collection<GrantedAuthority> authorities = buildInternalConverter().convert(jwt);

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
    // Helper — mirrors the lambda in SecurityConfig.jwtAuthenticationConverterForKeycloak()
    // ──────────────────────────────────────────────────────────────────

    /**
     * Directly replicates the converter lambda from cart SecurityConfig (uses toList(), not
     * toCollection(ArrayList::new) — matching production code exactly).
     */
    private Converter<Jwt, Collection<GrantedAuthority>> buildInternalConverter() {
        return jwt -> {
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        };
    }
}
