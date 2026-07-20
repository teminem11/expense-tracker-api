package com.artem.expensetracker.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void createsAndReadsToken() {
        JwtService service = new JwtService("a-very-long-test-secret-key-that-is-over-thirty-two-bytes", 60000);
        String token = service.generate("dev@example.com");
        assertThat(service.subject(token)).isEqualTo("dev@example.com");
    }
}
