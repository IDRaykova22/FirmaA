package com.firmaa.techdept.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Same secret and expiration as application.properties
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000);
    }

    @Test
    void generateJwtToken_returnsNonNullToken() {
        String token = jwtUtils.generateJwtToken("alice");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateJwtToken_tokenHasThreeParts() {
        String token = jwtUtils.generateJwtToken("alice");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must consist of header.payload.signature");
    }

    @Test
    void generateJwtToken_differentUsersProduceDifferentTokens() {
        String token1 = jwtUtils.generateJwtToken("alice");
        String token2 = jwtUtils.generateJwtToken("bob");
        assertNotEquals(token1, token2);
    }

    @Test
    void generateJwtToken_sameUserProducesUniqueTokensOverTime() throws InterruptedException {
        String token1 = jwtUtils.generateJwtToken("alice");
        Thread.sleep(10);
        String token2 = jwtUtils.generateJwtToken("alice");
        assertNotEquals(token1, token2, "Tokens issued at different times should differ");
    }
}
