package com.hub.synhub_backend.iam.infrastructure.tokens.jwt;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie de sesión (decisión HUM-04): el JWT viaja en una cookie {@code HttpOnly} válida por
 * {@code authorization.jwt.expiration.days} (1 día), así que el JavaScript del navegador nunca lo ve.
 * <p>
 * {@code SameSite=Strict} evita que otro sitio la use (CSRF) y {@code Path=/api} la limita a la API.
 * {@code Secure} se puede apagar solo para HTTP local ({@code authorization.cookie.secure=false}).
 */
@Component
public class SessionCookieService {

    public static final String COOKIE_NAME = "SYNHUB_SESSION";
    private static final String COOKIE_PATH = "/api";

    private final Duration lifetime;
    private final boolean secure;

    public SessionCookieService(
            @Value("${authorization.jwt.expiration.days}") int expirationDays,
            @Value("${authorization.cookie.secure:true}") boolean secure) {
        this.lifetime = Duration.ofDays(expirationDays);
        this.secure = secure;
    }

    /** Cookie que abre la sesión con el token recién emitido. */
    public ResponseCookie create(String token) {
        return base(token).maxAge(lifetime).build();
    }

    /** Cookie vacía y vencida: el navegador borra la sesión. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    /** Momento en que vence una sesión abierta ahora. */
    public Instant expiresAtFromNow() {
        return Instant.now().plus(lifetime);
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
