package com.example;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Test security configuration to enable method security in tests.
 * This ensures @RolesAllowed annotations work properly in service tests.
 */
@TestConfiguration
@EnableMethodSecurity(jsr250Enabled = true)
public class TestSecurityConfig {
    // Configuration only - beans are provided by main SecurityConfig
}
