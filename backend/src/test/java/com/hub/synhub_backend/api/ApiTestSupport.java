package com.hub.synhub_backend.api;

import org.springframework.beans.factory.annotation.Value;
import com.hub.synhub_backend.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base de las pruebas de API: levanta la aplicación completa en un puerto aleatorio contra un PostgreSQL en
 * Testcontainers (requiere Docker) y la ejercita por HTTP real, con el filtro JWT incluido.
 * Cada prueba crea sus propios usuarios con un sufijo aleatorio, así que no depende del estado de la base.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
abstract class ApiTestSupport {

    protected static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    /** Respuesta HTTP con el cuerpo ya parseado (null si está vacío o no es JSON). */
    protected record Res(int status, String raw, JsonNode json) {
        long id() { return json.get("id").asLong(); }
        String text(String field) { return json.get(field).asString(); }
    }

    /** Usuario registrado y autenticado. */
    protected record Account(long id, String username, String token) {}

    // ---- HTTP
    protected Res call(String method, String path, String token, Object body) {
        var uri = URI.create("http://localhost:" + port + (path.startsWith("/actuator") ? path : "/api/v1" + path));
        var builder = HttpRequest.newBuilder(uri).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
        try {
            var response = HTTP.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = null;
            try {
                if (!response.body().isBlank()) json = JSON.readTree(response.body());
            } catch (RuntimeException notJson) {
                // cuerpo de texto plano
            }
            return new Res(response.statusCode(), response.body(), json);
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + path + " falló", e);
        }
    }

    protected Res get(String path, String token) { return call("GET", path, token, null); }
    protected Res post(String path, String token, Object body) { return call("POST", path, token, body); }
    protected Res put(String path, String token, Object body) { return call("PUT", path, token, body); }
    protected Res delete(String path, String token) { return call("DELETE", path, token, null); }

    // ---- Datos de prueba
    protected static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static Map<String, Object> signUpBody(String username, String email, String password, Object roles) {
        var body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("name", username);
        body.put("surname", "Test");
        body.put("imgUrl", "https://example.com/avatar.png");
        body.put("email", email);
        body.put("password", password);
        body.put("accountRoles", roles);
        return body;
    }

    protected Account account(String prefix) {
        var username = unique(prefix);
        var signUp = post("/authentication/sign-up", null, signUpBody(username, username + "@synhub.test", "Secret123!", java.util.List.of("ROLE_USER")));
        if (signUp.status() != 201) throw new IllegalStateException("sign-up " + signUp.status() + ": " + signUp.raw());
        var signIn = post("/authentication/sign-in", null, Map.of("username", username, "password", "Secret123!"));
        return new Account(signIn.id(), username, signIn.text("token"));
    }

    protected long createGroup(Account leader) {
        var res = post("/groups", leader.token(), Map.of("name", unique("Grupo"), "description", "Grupo de pruebas", "imgUrl", ""));
        if (res.status() != 200) throw new IllegalStateException("crear grupo " + res.status() + ": " + res.raw());
        return res.id();
    }

    /** Invita y acepta: el usuario queda como GROUP_MEMBER. */
    protected void join(Account leader, Account member, long groupId) {
        var invitation = post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId));
        if (invitation.status() != 200) throw new IllegalStateException("invitar " + invitation.status() + ": " + invitation.raw());
        var accept = post("/invitations/accept?invitationId=" + invitation.id(), member.token(), null);
        if (accept.status() != 200) throw new IllegalStateException("aceptar " + accept.status() + ": " + accept.raw());
    }

    protected static Map<String, Object> taskBody(long groupId, long assigneeId, String title) {
        var body = new LinkedHashMap<String, Object>();
        body.put("title", title);
        body.put("description", "Descripción de prueba");
        body.put("dueDate", OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).toString());
        body.put("userId", assigneeId);
        body.put("groupId", groupId);
        return body;
    }

    protected long createTask(Account leader, long groupId, Account assignee) {
        var res = post("/tasks", leader.token(), taskBody(groupId, assignee.id(), unique("Tarea")));
        if (res.status() != 200) throw new IllegalStateException("crear tarea " + res.status() + ": " + res.raw());
        return res.id();
    }

    /** El asignado completa la tarea y envía la entrega; devuelve el id de la request. */
    protected long submit(Account member, long taskId) {
        var completed = put("/tasks/" + taskId + "/status?status=COMPLETED", member.token(), null);
        if (completed.status() != 200) throw new IllegalStateException("completar " + completed.status() + ": " + completed.raw());
        var request = post("/tasks/" + taskId + "/requests", member.token(), Map.of("description", "Listo", "requestType", "SUBMISSION"));
        if (request.status() != 201) throw new IllegalStateException("entregar " + request.status() + ": " + request.raw());
        return request.id();
    }

    /** Escenario base: líder, miembro y un usuario ajeno; grupo con el miembro unido. */
    protected record Team(Account leader, Account member, Account stranger, long groupId) {}

    protected Team team() {
        var leader = account("lider");
        var member = account("miembro");
        var stranger = account("ajeno");
        var groupId = createGroup(leader);
        join(leader, member, groupId);
        return new Team(leader, member, stranger, groupId);
    }
}
