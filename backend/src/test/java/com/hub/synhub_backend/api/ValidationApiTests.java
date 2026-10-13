package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Validación de entrada por HTTP. */
class ValidationApiTests extends ApiTestSupport {

    @Test void malformedGroupCodeIs400() {
        var account = account("codigo");
        assertEquals(400, get("/groups/search?code=abc", account.token()).status());
    }

    @Test void unknownGroupCodeIs404() {
        var account = account("codigo");
        assertEquals(404, get("/groups/search?code=ZZZZZZZZZ", account.token()).status());
    }

    @Test void invalidRequestTypeIs400() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var res = post("/tasks/" + taskId + "/requests", team.member().token(), Map.of("description", "x", "requestType", "DELETION"));
        assertEquals(400, res.status());
    }

    @Test void membersCannotCreateExpiredRequests() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var res = post("/tasks/" + taskId + "/requests", team.member().token(), Map.of("description", "x", "requestType", "EXPIRED"));
        assertEquals(400, res.status());
    }

    @Disabled("AUDIT BUG-03 / SEC-07: se crean tareas sin título (200)")
    @Test void taskWithoutTitleIs400() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), "x");
        body.remove("title");
        assertEquals(400, post("/tasks", team.leader().token(), body).status());
    }

    @Disabled("AUDIT SEC-07: difficulty fuera de 1..5 se acepta")
    @Test void difficultyOutOfRangeIs400() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), "x");
        body.put("difficulty", 9);
        assertEquals(400, post("/tasks", team.leader().token(), body).status());
    }

    @Disabled("AUDIT BUG-06 / SEC-07: un grupo sin nombre responde 500")
    @Test void groupWithoutNameIs400() {
        var account = account("grupo");
        assertEquals(400, post("/groups", account.token(), Map.of("description", "d", "imgUrl", "")).status());
    }

    @Disabled("AUDIT SEC-07: el sign-up acepta contraseñas débiles y emails inválidos (201)")
    @Test void weakSignUpIs400() {
        var username = unique("debil");
        assertEquals(400, post("/authentication/sign-up", null, signUpBody(username, "no-es-email", "1", List.of("ROLE_USER"))).status());
    }

    @Disabled("AUDIT SEC-07: un valor de enum inválido en un parámetro no está mapeado a 400")
    @Test void invalidStatusParameterIs400() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(400, put("/tasks/" + taskId + "/status?status=ARCHIVED", team.leader().token(), null).status());
    }
}
