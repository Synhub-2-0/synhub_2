package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** Endurecimiento de la configuración HTTP: CORS, Swagger y endpoints de depuración. */
class HardeningApiTests extends ApiTestSupport {

    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> preflight(String origin) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/groups"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    // AUDIT SEC-08
    @Test
    void corsOnlyAllowsConfiguredOrigins() throws Exception {
        var evil = preflight("https://evil.example");
        assertTrue(
                evil.headers().firstValue("Access-Control-Allow-Origin").isEmpty(), "un origen ajeno no recibe CORS");

        var front = preflight("http://localhost:4200");
        assertEquals(
                "http://localhost:4200",
                front.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals(
                "true",
                front.headers().firstValue("Access-Control-Allow-Credentials").orElse(null));
    }

    // AUDIT SEC-08: Swagger solo con el perfil dev
    @Test
    void swaggerIsDisabledOutsideDev() {
        assertNotEquals(200, call("GET", "/../../v3/api-docs", null, null).status());
        var token = account("docs").token();
        assertNotEquals(200, call("GET", "/../../v3/api-docs", token, null).status());
    }

    // AUDIT SEC-09: los endpoints de depuración ya no existen
    @Test
    void debugEndpointsAreGone() {
        var token = account("debug").token();
        assertEquals(404, post("/tests/token", token, null).status());
        assertEquals(404, get("/tests/fetch-user", token).status());
    }

    // Rutas o métodos inexistentes respondían 500 por el handler genérico.
    @Test
    void unknownRoutesAndMethodsKeepTheirStatus() {
        var token = account("rutas").token();
        var notFound = get("/no-existe", token);
        assertEquals(404, notFound.status());
        assertTrue(notFound.text("timestamp").endsWith("Z"), "AUDIT BUG-14: timestamp en UTC con zona");
        assertEquals(405, call("PATCH", "/users/me", token, null).status());
    }
}
