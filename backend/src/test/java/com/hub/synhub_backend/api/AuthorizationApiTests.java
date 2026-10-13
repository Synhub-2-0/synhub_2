package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Matriz de permisos por HTTP: líder, miembro y un usuario ajeno al grupo.
 * Las pruebas habilitadas describen el comportamiento actual correcto; las deshabilitadas, el esperado tras la auditoría.
 */
class AuthorizationApiTests extends ApiTestSupport {

    // ---- Vigentes
    @Test void memberCannotEditOrDeleteTheGroup() {
        var team = team();
        assertEquals(403, put("/groups/" + team.groupId(), team.member().token(), Map.of("name", "X", "description", "", "imgUrl", "")).status());
        assertEquals(403, delete("/groups/" + team.groupId(), team.member().token()).status());
    }

    @Test void memberCannotInvite() {
        var team = team();
        var res = post("/invitations", team.member().token(), Map.of("userId", team.stranger().id(), "groupId", team.groupId()));
        assertEquals(403, res.status());
    }

    @Test void memberCannotCreateEditOrDeleteTasks() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(403, post("/tasks", team.member().token(), taskBody(team.groupId(), team.member().id(), "x")).status());
        assertEquals(403, put("/tasks/" + taskId, team.member().token(), Map.of("title", "x", "description", "", "userId", team.member().id())).status());
        assertEquals(403, delete("/tasks/" + taskId, team.member().token()).status());
    }

    @Test void onlyTheLeaderReviewsSubmissions() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(403, put("/tasks/" + taskId + "/requests/" + requestId + "/status/APPROVED", team.member().token(), null).status());
        assertEquals(403, get("/groups/" + team.groupId() + "/requests", team.member().token()).status());
    }

    @Test void strangerCannotChangeTaskStatus() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(403, put("/tasks/" + taskId + "/status?status=COMPLETED", team.stranger().token(), null).status());
    }

    @Test void onlyTheLeaderGetsTheAiReport() {
        var team = team();
        assertEquals(403, get("/ai/groups/" + team.groupId() + "/report", team.member().token()).status());
    }

    @Test void onlyTheInvitedOrTheLeaderCanCancelAnInvitation() {
        var team = team();
        var guest = account("invitado");
        var invitation = post("/invitations", team.leader().token(), Map.of("userId", guest.id(), "groupId", team.groupId()));
        assertEquals(403, delete("/invitations/" + invitation.id(), team.stranger().token()).status());
        assertEquals(403, post("/invitations/accept?invitationId=" + invitation.id(), team.stranger().token(), null).status());
    }

    // ---- Esperados (auditoría)
    @Disabled("AUDIT SEC-01: las lecturas de un grupo no verifican la pertenencia del solicitante")
    @Test void strangerCannotReadAnotherGroup() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        var token = team.stranger().token();
        assertAll(
                () -> assertEquals(403, get("/groups/" + team.groupId(), token).status(), "GET /groups/{id}"),
                () -> assertEquals(403, get("/groups/" + team.groupId() + "/members", token).status(), "GET members"),
                () -> assertEquals(403, get("/invitations/group?groupId=" + team.groupId(), token).status(), "GET invitations/group"),
                () -> assertEquals(403, get("/tasks/" + taskId, token).status(), "GET /tasks/{id}"),
                () -> assertEquals(403, get("/tasks/group/" + team.groupId(), token).status(), "GET /tasks/group/{id}"),
                () -> assertEquals(403, get("/tasks/group/" + team.groupId() + "/user/" + team.member().id(), token).status(), "GET tasks of member"),
                () -> assertEquals(403, get("/tasks/" + taskId + "/requests", token).status(), "GET task requests"));
    }

    @Disabled("AUDIT SEC-01: cualquier miembro (no solo el líder) ve las invitaciones del grupo")
    @Test void onlyTheLeaderSeesPendingInvitations() {
        var team = team();
        assertEquals(200, get("/invitations/group?groupId=" + team.groupId(), team.leader().token()).status());
        assertEquals(403, get("/invitations/group?groupId=" + team.groupId(), team.member().token()).status());
    }

    @Disabled("AUDIT SEC-02: DELETE /tasks/{t}/requests/{r} no verifica permisos")
    @Test void strangerCannotDeleteRequests() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(403, delete("/tasks/" + taskId + "/requests/" + requestId, team.stranger().token()).status());
        assertEquals(200, get("/tasks/" + taskId + "/requests/" + requestId, team.leader().token()).status(), "la request sigue existiendo");
    }

    @Disabled("AUDIT SEC-06: GET /users devuelve todos los usuarios con su email a cualquier usuario")
    @Test void userDirectoryIsNotPublic() {
        var team = team();
        assertEquals(403, get("/users", team.stranger().token()).status());
        var other = get("/users/" + team.member().id(), team.stranger().token());
        assertNull(other.json().get("email"), "no se expone el email de otros usuarios");
    }
}
