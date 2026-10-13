package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Registro, login, token y endpoints públicos por HTTP. */
class AuthApiTests extends ApiTestSupport {

    @Test void signUpCreatesUserWithoutExposingThePassword() {
        var username = unique("nuevo");
        var res = post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(201, res.status(), res.raw());
        assertEquals(username, res.text("username"));
        assertFalse(res.raw().contains("Secret123!"));
        assertNull(res.json().get("password"));
    }

    @Test void signInReturnsTokenAndMeIdentifiesTheUser() {
        var account = account("login");
        var me = get("/users/me", account.token());
        assertEquals(200, me.status());
        assertEquals(account.username(), me.text("username"));
        assertEquals(account.id(), me.id());
    }

    @Test void wrongPasswordIs401() {
        var account = account("login");
        var res = post("/authentication/sign-in", null, Map.of("username", account.username(), "password", "incorrecta"));
        assertEquals(401, res.status());
    }

    @Test void duplicateUsernameIs409() {
        var account = account("dup");
        var res = post("/authentication/sign-up", null, signUpBody(account.username(), unique("x") + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(409, res.status());
    }

    @Test void protectedEndpointsNeedAValidToken() {
        var account = account("token");
        assertEquals(401, get("/users/me", null).status());
        assertEquals(401, get("/users/me", tamper(account.token())).status());
        assertEquals(401, get("/users/me", "no-es-un-jwt").status());
    }

    /** Cambia un carácter del payload: la firma deja de coincidir. */
    private static String tamper(String token) {
        var parts = token.split("\\.");
        var payload = parts[1].toCharArray();
        payload[payload.length / 2] = payload[payload.length / 2] == 'A' ? 'B' : 'A';
        return parts[0] + "." + new String(payload) + "." + parts[2];
    }

    @Test void healthIsPublic() {
        var res = get("/actuator/health", null);
        assertEquals(200, res.status());
        assertEquals("UP", res.text("status"));
    }

    // AUDIT SEC-05 (resuelto)
    @Test void publicSignUpCannotGrantAdmin() {
        var username = unique("admin");
        var res = post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_ADMIN")));
        assertEquals(201, res.status());
        assertEquals("[\"ROLE_USER\"]", res.json().get("accountRoles").toString());
    }

    // AUDIT BUG-04 (resuelto)
    @Test void duplicateEmailIs409() {
        var account = account("mail");
        var res = post("/authentication/sign-up", null, signUpBody(unique("otro"), account.username() + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(409, res.status(), res.raw());
    }

    // AUDIT BUG-05 (resuelto)
    @Test void unknownRoleIsNotAServerError() {
        var username = unique("rol");
        var res = post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_ROOT")));
        assertTrue(res.status() == 201 || res.status() == 400, "esperado 201 (rol ignorado) o 400, fue " + res.status());
    }
}
