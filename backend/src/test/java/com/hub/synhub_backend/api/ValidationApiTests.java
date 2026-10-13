package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Validación de entrada por HTTP. */
class ValidationApiTests extends ApiTestSupport {

    @Test
    void malformedGroupCodeIs400() {
        var account = account("codigo");
        assertEquals(400, get("/groups/search?code=abc", account.token()).status());
    }

    @Test
    void unknownGroupCodeIs404() {
        var account = account("codigo");
        assertEquals(404, get("/groups/search?code=ZZZZZZZZZ", account.token()).status());
    }

    @Test
    void invalidRequestTypeIs400() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var res = post(
                "/tasks/" + taskId + "/requests",
                team.member().token(),
                Map.of("description", "x", "requestType", "DELETION"));
        assertEquals(400, res.status());
    }

    @Test
    void membersCannotCreateExpiredRequests() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var res = post(
                "/tasks/" + taskId + "/requests",
                team.member().token(),
                Map.of("description", "x", "requestType", "EXPIRED"));
        assertEquals(400, res.status());
    }

    // AUDIT BUG-03 / SEC-07 (resuelto)
    @Test
    void taskWithoutTitleIs400() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), "x");
        body.remove("title");
        assertEquals(400, post("/tasks", team.leader().token(), body).status());
    }

    // AUDIT SEC-07 (resuelto)
    @Test
    void difficultyOutOfRangeIs400() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), "x");
        body.put("difficulty", 9);
        assertEquals(400, post("/tasks", team.leader().token(), body).status());
    }

    // AUDIT BUG-06 / SEC-07 (resuelto)
    @Test
    void groupWithoutNameIs400() {
        var account = account("grupo");
        assertEquals(
                400,
                post("/groups", account.token(), Map.of("description", "d", "imgUrl", ""))
                        .status());
    }

    // AUDIT SEC-07 (resuelto)
    @Test
    void weakSignUpIs400() {
        var username = unique("debil");
        assertEquals(
                400,
                post("/authentication/sign-up", null, signUpBody(username, "no-es-email", "1", List.of("ROLE_USER")))
                        .status());
    }

    // AUDIT SEC-07 (resuelto)
    @Test
    void invalidStatusParameterIs400() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                400,
                put(
                                "/tasks/" + taskId + "/status?status=ARCHIVED",
                                team.leader().token(),
                                null)
                        .status());
    }

    // ---- AUDIT SEC-07: entradas que antes terminaban en 500
    @Test
    void validationErrorListsTheInvalidFields() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), "x");
        body.remove("title");
        body.remove("dueDate");
        var res = post("/tasks", team.leader().token(), body);
        assertEquals(400, res.status());
        assertTrue(res.text("message").contains("title") && res.text("message").contains("dueDate"), res.raw());
    }

    @Test
    void signUpWithoutImageGetsTheDefaultAvatar() {
        var username = unique("sinimg");
        var body = signUpBody(username, username + "@synhub.test", "Secret123!", List.of());
        body.put("imgUrl", "");
        var res = post("/authentication/sign-up", null, body);
        assertEquals(201, res.status(), res.raw());
        assertEquals("/default-avatar.jpg", res.text("imgUrl"));
    }

    @Test
    void groupWithoutDescriptionIsCreated() {
        var account = account("grupo");
        assertEquals(
                200,
                post("/groups", account.token(), Map.of("name", "Sin descripción", "imgUrl", ""))
                        .status());
    }

    @Test
    void partialGroupUpdateKeepsTheOtherFields() {
        var leader = account("lider");
        var groupId = createGroup(leader);
        var res = put("/groups/" + groupId, leader.token(), Map.of("name", "Renombrado"));
        assertEquals(200, res.status(), res.raw());
        assertEquals("Renombrado", res.text("name"));
        assertEquals("Grupo de pruebas", res.text("description"));
    }

    @Test
    void malformedJsonIs400() {
        var account = account("json");
        assertEquals(
                400, call("POST", "/groups", account.token(), "no-es-un-objeto").status());
    }

    @Test
    void missingRequiredParameterIs400() {
        var account = account("param");
        assertEquals(400, get("/groups/user/role", account.token()).status());
    }
}
