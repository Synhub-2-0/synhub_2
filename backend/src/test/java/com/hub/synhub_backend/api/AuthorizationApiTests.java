package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Matriz de permisos por HTTP: líder, miembro y un usuario ajeno al grupo.
 * Las pruebas habilitadas describen el comportamiento actual correcto; las deshabilitadas, el esperado tras la auditoría.
 */
class AuthorizationApiTests extends ApiTestSupport {

    // ---- Vigentes
    @Test
    void memberCannotEditOrDeleteTheGroup() {
        var team = team();
        assertEquals(
                403,
                put(
                                "/groups/" + team.groupId(),
                                team.member().token(),
                                Map.of("name", "X", "description", "", "imgUrl", ""))
                        .status());
        assertEquals(
                403, delete("/groups/" + team.groupId(), team.member().token()).status());
    }

    @Test
    void memberCannotInvite() {
        var team = team();
        var res = post(
                "/invitations",
                team.member().token(),
                Map.of("userId", team.stranger().id(), "groupId", team.groupId()));
        assertEquals(403, res.status());
    }

    @Test
    void memberCannotCreateEditOrDeleteTasks() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                403,
                post(
                                "/tasks",
                                team.member().token(),
                                taskBody(team.groupId(), team.member().id(), "x"))
                        .status());
        assertEquals(
                403,
                put(
                                "/tasks/" + taskId,
                                team.member().token(),
                                Map.of(
                                        "title",
                                        "x",
                                        "description",
                                        "",
                                        "userId",
                                        team.member().id()))
                        .status());
        assertEquals(403, delete("/tasks/" + taskId, team.member().token()).status());
    }

    @Test
    void onlyTheLeaderReviewsSubmissions() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(
                403,
                put(
                                "/tasks/" + taskId + "/requests/" + requestId + "/status/APPROVED",
                                team.member().token(),
                                null)
                        .status());
        assertEquals(
                403,
                get("/groups/" + team.groupId() + "/requests", team.member().token())
                        .status());
    }

    @Test
    void strangerCannotChangeTaskStatus() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                403,
                put(
                                "/tasks/" + taskId + "/status?status=COMPLETED",
                                team.stranger().token(),
                                null)
                        .status());
    }

    @Test
    void onlyTheLeaderGetsTheAiReport() {
        var team = team();
        assertEquals(
                403,
                get("/ai/groups/" + team.groupId() + "/report", team.member().token())
                        .status());
    }

    @Test
    void onlyTheInvitedOrTheLeaderCanCancelAnInvitation() {
        var team = team();
        var guest = account("invitado");
        var invitation =
                post("/invitations", team.leader().token(), Map.of("userId", guest.id(), "groupId", team.groupId()));
        assertEquals(
                403,
                delete("/invitations/" + invitation.id(), team.stranger().token())
                        .status());
        assertEquals(
                403,
                post(
                                "/invitations/accept?invitationId=" + invitation.id(),
                                team.stranger().token(),
                                null)
                        .status());
    }

    // ---- Esperados (auditoría)
    // AUDIT SEC-01 (resuelto)
    @Test
    void strangerCannotReadAnotherGroup() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        var token = team.stranger().token();
        assertAll(
                () -> assertEquals(403, get("/groups/" + team.groupId(), token).status(), "GET /groups/{id}"),
                () -> assertEquals(
                        403,
                        get("/groups/" + team.groupId() + "/members", token).status(),
                        "GET members"),
                () -> assertEquals(
                        403,
                        get("/invitations/group?groupId=" + team.groupId(), token)
                                .status(),
                        "GET invitations/group"),
                () -> assertEquals(403, get("/tasks/" + taskId, token).status(), "GET /tasks/{id}"),
                () -> assertEquals(
                        403, get("/tasks/group/" + team.groupId(), token).status(), "GET /tasks/group/{id}"),
                () -> assertEquals(
                        403,
                        get(
                                        "/tasks/group/" + team.groupId() + "/user/"
                                                + team.member().id(),
                                        token)
                                .status(),
                        "GET tasks of member"),
                () -> assertEquals(
                        403, get("/tasks/" + taskId + "/requests", token).status(), "GET task requests"));
    }

    // AUDIT SEC-01 (resuelto)
    @Test
    void onlyTheLeaderSeesPendingInvitations() {
        var team = team();
        assertEquals(
                200,
                get(
                                "/invitations/group?groupId=" + team.groupId(),
                                team.leader().token())
                        .status());
        assertEquals(
                403,
                get(
                                "/invitations/group?groupId=" + team.groupId(),
                                team.member().token())
                        .status());
    }

    // AUDIT SEC-02 (resuelto)
    @Test
    void strangerCannotDeleteRequests() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(
                403,
                delete(
                                "/tasks/" + taskId + "/requests/" + requestId,
                                team.stranger().token())
                        .status());
        assertEquals(
                200,
                get("/tasks/" + taskId + "/requests/" + requestId, team.leader().token())
                        .status(),
                "la request sigue existiendo");
    }

    // AUDIT SEC-06 (resuelto) + HUM-06: los demás ven nombre, correo y foto, pero no los roles
    @Test
    void userDirectoryIsNotPublic() {
        var team = team();
        assertEquals(403, get("/users", team.stranger().token()).status());
        var other = get("/users/" + team.member().id(), team.stranger().token());
        assertEquals(200, other.status());
        assertNotNull(other.json().get("name"));
        assertNotNull(other.json().get("email"));
        assertNotNull(other.json().get("imgUrl"));
        assertNull(other.json().get("accountRoles"), "no se exponen los roles de otros usuarios");
        var me = get("/users/me", team.member().token());
        assertEquals("[\"ROLE_USER\"]", me.json().get("accountRoles").toString());
    }

    @Test
    void nestedUsersDoNotExposeRoles() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var task = get("/tasks/" + taskId, team.member().token()).json();
        assertNotNull(task.get("assignedTo").get("email"));
        assertNull(task.get("assignedTo").get("accountRoles"));
        assertNull(task.get("group").get("leader").get("accountRoles"));
    }
}
