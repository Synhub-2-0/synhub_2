package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Registro, login, token y endpoints públicos por HTTP. */
class AuthApiTests extends ApiTestSupport {

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    private int port;

    @Test
    void signUpCreatesUserWithoutExposingThePassword() {
        var username = unique("nuevo");
        var res = post(
                "/authentication/sign-up",
                null,
                signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(201, res.status(), res.raw());
        assertEquals(username, res.text("username"));
        assertFalse(res.raw().contains("Secret123!"));
        assertNull(res.json().get("password"));
    }

    @Test
    void signInReturnsTokenAndMeIdentifiesTheUser() {
        var account = account("login");
        var me = get("/users/me", account.token());
        assertEquals(200, me.status());
        assertEquals(account.username(), me.text("username"));
        assertEquals(account.id(), me.id());
    }

    // HUM-04: la sesión es una cookie HttpOnly de 1 día y el token no aparece en el cuerpo
    @Test
    void signInOpensAnHttpOnlySessionCookie() {
        var username = unique("cookie");
        post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", null));
        var signIn = post("/authentication/sign-in", null, Map.of("username", username, "password", "Secret123!"));
        assertEquals(200, signIn.status());
        assertNull(signIn.json().get("token"), "el token no se expone al JavaScript");
        var cookie = signIn.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith(SESSION_COOKIE + "="));
        for (var attribute : List.of("HttpOnly", "Secure", "SameSite=Strict", "Path=/api", "Max-Age=86400")) {
            assertTrue(cookie.contains(attribute), cookie + " sin " + attribute);
        }
        var expiresAt = java.time.Instant.parse(signIn.text("expiresAt"));
        var inOneDay = java.time.Instant.now().plus(java.time.Duration.ofDays(1));
        assertTrue(
                Math.abs(java.time.Duration.between(expiresAt, inOneDay).toSeconds()) < 60,
                "expiresAt = ahora + 1 día");
    }

    @Test
    void signOutClearsTheCookie() {
        var account = account("salir");
        var res = post("/authentication/sign-out", account.token(), null);
        assertEquals(204, res.status());
        var cookie = res.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith(SESSION_COOKIE + "=;") && cookie.contains("Max-Age=0"), cookie);
    }

    @Test
    void bearerHeaderStillWorksForApiClients() throws Exception {
        var account = account("bearer");
        var request = java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("http://localhost:" + port + "/api/v1/users/me"))
                .header("Authorization", "Bearer " + account.token())
                .build();
        var response = java.net.http.HttpClient.newHttpClient()
                .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void wrongPasswordIs401() {
        var account = account("login");
        var res =
                post("/authentication/sign-in", null, Map.of("username", account.username(), "password", "incorrecta"));
        assertEquals(401, res.status());
    }

    @Test
    void duplicateUsernameIs409() {
        var account = account("dup");
        var res = post(
                "/authentication/sign-up",
                null,
                signUpBody(account.username(), unique("x") + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(409, res.status());
    }

    @Test
    void protectedEndpointsNeedAValidToken() {
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

    @Test
    void healthIsPublic() {
        var res = get("/actuator/health", null);
        assertEquals(200, res.status());
        assertEquals("UP", res.text("status"));
    }

    // AUDIT SEC-05 (resuelto)
    @Test
    void publicSignUpCannotGrantAdmin() {
        var username = unique("admin");
        var res = post(
                "/authentication/sign-up",
                null,
                signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_ADMIN")));
        assertEquals(201, res.status());
        assertEquals("[\"ROLE_USER\"]", res.json().get("accountRoles").toString());
    }

    // AUDIT BUG-04 (resuelto)
    @Test
    void duplicateEmailIs409() {
        var account = account("mail");
        var res = post(
                "/authentication/sign-up",
                null,
                signUpBody(unique("otro"), account.username() + "@synhub.test", "Secret123!", List.of("ROLE_USER")));
        assertEquals(409, res.status(), res.raw());
    }

    // AUDIT BUG-05 (resuelto)
    @Test
    void unknownRoleIsNotAServerError() {
        var username = unique("rol");
        var res = post(
                "/authentication/sign-up",
                null,
                signUpBody(username, username + "@synhub.test", "Secret123!", List.of("ROLE_ROOT")));
        assertTrue(
                res.status() == 201 || res.status() == 400, "esperado 201 (rol ignorado) o 400, fue " + res.status());
    }
}
