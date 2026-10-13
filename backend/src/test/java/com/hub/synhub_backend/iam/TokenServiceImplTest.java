package com.hub.synhub_backend.iam;

import com.hub.synhub_backend.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/** Emisión y validación de JWT. */
class TokenServiceImplTest {

    private static final String SECRET = "test-secret-for-synhub-jwt-signing-0123456789abcdef";

    private TokenServiceImpl service;

    private static TokenServiceImpl tokenService(String secret, int expirationDays) {
        var service = new TokenServiceImpl();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationDays", expirationDays);
        return service;
    }

    @BeforeEach
    void setUp() {
        service = tokenService(SECRET, 7);
    }

    @Test void generatedTokenIsValidAndCarriesTheUsername() {
        var token = service.generateToken("ana");
        assertTrue(service.validateToken(token));
        assertEquals("ana", service.getUsernameFromToken(token));
    }

    @Test void tamperedTokenIsRejected() {
        var token = service.generateToken("ana");
        var last = token.charAt(token.length() - 1);
        var tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');
        assertFalse(service.validateToken(tampered));
    }

    @Test void tokenSignedWithAnotherKeyIsRejected() {
        var other = tokenService("another-secret-for-synhub-jwt-signing-0123456789", 7);
        assertFalse(service.validateToken(other.generateToken("ana")));
    }

    @Test void expiredTokenIsRejected() {
        var expired = tokenService(SECRET, -1).generateToken("ana");
        assertFalse(service.validateToken(expired));
    }

    @Test void garbageIsRejected() {
        assertFalse(service.validateToken("not-a-jwt"));
        assertFalse(service.validateToken(""));
    }

    @Test void extractsBearerTokenFromHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def.ghi");
        assertEquals("abc.def.ghi", service.getBearerTokenFrom(request));
    }

    @Test void ignoresMissingOrNonBearerHeader() {
        assertNull(service.getBearerTokenFrom(new MockHttpServletRequest()));
        var basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        assertNull(service.getBearerTokenFrom(basic));
    }
}
