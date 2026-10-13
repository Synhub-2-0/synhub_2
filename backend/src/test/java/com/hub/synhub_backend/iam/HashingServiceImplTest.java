package com.hub.synhub_backend.iam;

import com.hub.synhub_backend.iam.infrastructure.hashing.bcrypt.services.HashingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hash de contraseñas con BCrypt. */
class HashingServiceImplTest {

    private HashingServiceImpl hashing;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = HashingServiceImpl.class.getDeclaredConstructor();
        constructor.setAccessible(true); // el constructor es package-private: Spring lo instancia
        hashing = constructor.newInstance();
    }

    @Test void encodeDoesNotStorePlainTextAndIsSalted() {
        var first = hashing.encode("Secret123!");
        assertNotEquals("Secret123!", first);
        assertTrue(first.startsWith("$2"));
        assertNotEquals(first, hashing.encode("Secret123!"));
    }

    @Test void matchesOnlyTheOriginalPassword() {
        var hash = hashing.encode("Secret123!");
        assertTrue(hashing.matches("Secret123!", hash));
        assertFalse(hashing.matches("secret123!", hash));
    }
}
