package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

/**
 * Producción: sin orígenes CORS configurados, porque front y API comparten origen detrás de un proxy.
 * El proxy puede reescribir el Host (nginx con $host, CloudFront), así que el Origin del navegador no
 * coincide con el Host que ve Spring. Eso no debe bloquear el login (regresión de SEC-08 vista en Docker).
 */
@TestPropertySource(properties = "app.cors.allowed-origins=")
class SameOriginApiTests extends ApiTestSupport {

    @Value("${local.server.port}")
    private int port;

    @Test
    void signInBehindAProxyIsNotRejectedAsCrossOrigin() throws Exception {
        var username = unique("proxy");
        post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", null));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/authentication/sign-in"))
                .header("Content-Type", "application/json")
                .header("Origin", "https://synhub.example")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + username + "\",\"password\":\"Secret123!\"}"))
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(
                response.headers().firstValue("Access-Control-Allow-Origin").isEmpty(),
                "sin CORS no se autoriza a otros orígenes");
    }
}
