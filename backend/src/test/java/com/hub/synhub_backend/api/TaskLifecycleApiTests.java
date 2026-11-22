package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** Flujos completos por HTTP: grupo, invitación, tarea, entrega y validación. */
class TaskLifecycleApiTests extends ApiTestSupport {

    private static boolean containsId(tools.jackson.databind.JsonNode array, long id) {
        return StreamSupport.stream(array.spliterator(), false)
                .anyMatch(node -> node.get("id").asLong() == id);
    }

    @Test
    void invitationFlowAddsMember() {
        var leader = account("lider");
        var member = account("miembro");
        var groupId = createGroup(leader);
        assertTrue(containsId(
                get("/groups/user/role?groupRole=GROUP_LEADER", leader.token()).json(), groupId));

        var invitation = post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId));
        assertEquals(200, invitation.status());
        assertTrue(containsId(get("/invitations/user", member.token()).json(), invitation.id()));
        assertEquals(
                409,
                post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId))
                        .status(),
                "invitación duplicada");

        assertEquals(
                200,
                post("/invitations/accept?invitationId=" + invitation.id(), member.token(), null)
                        .status());

        var members = get("/groups/" + groupId + "/members", leader.token());
        assertEquals(2, members.json().size());
        assertTrue(containsId(
                get("/groups/user/role?groupRole=GROUP_MEMBER", member.token()).json(), groupId));
        assertEquals(0, get("/invitations/user", member.token()).json().size());
        assertEquals(
                409,
                post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId))
                        .status(),
                "ya es miembro");
    }

    @Test
    void declinedInvitationDisappears() {
        var leader = account("lider");
        var guest = account("invitado");
        var groupId = createGroup(leader);
        var invitation = post("/invitations", leader.token(), Map.of("userId", guest.id(), "groupId", groupId));

        assertEquals(
                204, delete("/invitations/" + invitation.id(), guest.token()).status());
        assertEquals(0, get("/invitations/user", guest.token()).json().size());
        assertEquals(
                1, get("/groups/" + groupId + "/members", leader.token()).json().size());
    }

    @Test
    void approvedSubmissionMarksTaskDone() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                "IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));

        var requestId = submit(team.member(), taskId);
        assertEquals(
                409,
                post(
                                "/requests",
                                team.member().token(),
                                Map.of("taskId", taskId, "description", "otra", "requestType", "SUBMISSION"))
                        .status(),
                "solo una entrega pendiente");

        var inbox = get(
                "/requests/group?groupId=" + team.groupId() + "&status=PENDING&type=SUBMISSION",
                team.leader().token());
        assertEquals(200, inbox.status());
        assertTrue(containsId(inbox.json(), requestId));

        var approved =
                put("/requests/" + requestId + "/status/APPROVED", team.leader().token(), null);
        assertEquals(200, approved.status(), approved.raw());
        assertEquals("APPROVED", approved.text("requestStatus"));
        assertEquals("DONE", get("/tasks/" + taskId, team.leader().token()).text("status"));
        assertEquals(
                409,
                put("/requests/" + requestId + "/status/REJECTED", team.leader().token(), null)
                        .status(),
                "una request se revisa una sola vez");
        assertFalse(containsId(
                get(
                                "/requests/group?groupId=" + team.groupId() + "&status=PENDING",
                                team.leader().token())
                        .json(),
                requestId));
    }

    // AUDIT BUG-18: una tarea DONE es definitiva también para la API (la web ya lo impedía)
    @Test
    void doneTaskCannotBeEditedMovedOrDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(
                200,
                put("/requests/" + requestId + "/status/APPROVED", team.leader().token(), null)
                        .status());

        assertEquals(
                409,
                put(
                                "/tasks/" + taskId,
                                team.leader().token(),
                                Map.of("title", "Otra", "userId", team.member().id()))
                        .status());
        assertEquals(
                409,
                put(
                                "/tasks/" + taskId + "/status?status=IN_PROGRESS",
                                team.leader().token(),
                                null)
                        .status());
        assertEquals(409, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(
                403, delete("/tasks/" + taskId, team.member().token()).status(), "los permisos se revisan primero");
        assertEquals("DONE", get("/tasks/" + taskId, team.leader().token()).text("status"));
        assertEquals(
                204,
                delete("/groups/" + team.groupId(), team.leader().token()).status(),
                "borrar el grupo sí borra sus tareas DONE");
    }

    @Test
    void rejectedSubmissionReopensTask() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);

        var rejected =
                put("/requests/" + requestId + "/status/REJECTED", team.leader().token(), null);

        assertEquals(200, rejected.status());
        assertEquals(
                "IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));
    }

    @Test
    void taskCannotBeMarkedDoneDirectly() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                409,
                put("/tasks/" + taskId + "/status?status=DONE", team.leader().token(), null)
                        .status());
    }

    @Test
    void submissionNeedsAnInProgressOrCompletedTask() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                200,
                put("/tasks/" + taskId + "/status?status=ON_HOLD", team.leader().token(), null)
                        .status());
        var res = post(
                "/requests",
                team.member().token(),
                Map.of("taskId", taskId, "description", "x", "requestType", "SUBMISSION"));
        assertEquals(409, res.status());
    }

    @Test
    void submittingMarksTheTaskCompleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        assertEquals("COMPLETED", get("/tasks/" + taskId, team.leader().token()).text("status"));
    }

    /** El integrante pide una modificación: la tarea queda en espera hasta que el líder la edita. */
    private long requestModification(Team team, long taskId) {
        var request = post(
                "/requests",
                team.member().token(),
                Map.of("taskId", taskId, "description", "Cambiar el alcance", "requestType", "MODIFICATION"));
        assertEquals(201, request.status(), request.raw());
        return request.id();
    }

    private Map<String, Object> edit(Team team, String title) {
        return Map.of(
                "title",
                title,
                "description",
                "Descripción de prueba",
                "userId",
                team.member().id());
    }

    @Test
    void aModificationRequestPutsTheTaskOnHoldUntilTheLeaderEditsIt() throws InterruptedException {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = requestModification(team, taskId);

        assertEquals("ON_HOLD", get("/tasks/" + taskId, team.leader().token()).text("status"));
        assertEquals(
                "PENDING", get("/requests/" + requestId, team.leader().token()).text("requestStatus"));

        // Sin editar no se resuelve: ni reanudar a mano, ni aprobar, ni rechazar, ni borrar.
        assertEquals(
                409,
                put(
                                "/tasks/" + taskId + "/status?status=IN_PROGRESS",
                                team.leader().token(),
                                null)
                        .status());
        assertEquals(
                409,
                put("/requests/" + requestId + "/status/APPROVED", team.leader().token(), null)
                        .status());
        assertEquals(
                409,
                put("/requests/" + requestId + "/status/REJECTED", team.leader().token(), null)
                        .status());
        assertEquals(
                409, delete("/requests/" + requestId, team.leader().token()).status());
        assertEquals(
                409, delete("/requests/" + requestId, team.member().token()).status());
        assertEquals(
                409,
                post(
                                "/requests",
                                team.member().token(),
                                Map.of("taskId", taskId, "description", "x", "requestType", "SUBMISSION"))
                        .status(),
                "en espera no se puede entregar");
        assertEquals("ON_HOLD", get("/tasks/" + taskId, team.leader().token()).text("status"));

        // Un PUT que no cambia nada tampoco la resuelve.
        var task = get("/tasks/" + taskId, team.leader().token());
        var unchanged = new java.util.LinkedHashMap<String, Object>();
        unchanged.put("title", task.text("title"));
        unchanged.put("description", task.text("description"));
        unchanged.put("dueDate", task.text("dueDate"));
        unchanged.put("userId", team.member().id());
        assertEquals(
                200, put("/tasks/" + taskId, team.leader().token(), unchanged).status());
        assertEquals("ON_HOLD", get("/tasks/" + taskId, team.leader().token()).text("status"));

        // Editar la tarea la reanuda y cierra la solicitud.
        Thread.sleep(1500); // tiempo en espera: no debe contarse
        var edited = put("/tasks/" + taskId, team.leader().token(), edit(team, "Tarea con nuevo alcance"));
        assertEquals(200, edited.status(), edited.raw());
        assertEquals("IN_PROGRESS", edited.text("status"));
        assertEquals(
                "APPROVED", get("/requests/" + requestId, team.leader().token()).text("requestStatus"));

        // El integrante ya puede entregar; el tiempo medido no incluye la espera de 1,5 s.
        submit(team.member(), taskId);
        var time =
                get("/metrics/tasks/" + taskId + "/time-passed", team.leader().token());
        assertEquals(200, time.status(), time.raw());
        assertTrue(time.json().get("timePassed").asLong() < 1500, "el tiempo en espera no cuenta: " + time.raw());
    }

    @Test
    void onlyTheAssignedMemberRequestsAModificationAndOnlyWhileInProgress() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var body = Map.of("taskId", taskId, "description", "Cambiar", "requestType", "MODIFICATION");

        assertEquals(403, post("/requests", team.leader().token(), body).status());
        assertEquals(403, post("/requests", team.stranger().token(), body).status());
        assertEquals(
                400,
                post(
                                "/requests",
                                team.member().token(),
                                Map.of("taskId", taskId, "description", "  ", "requestType", "MODIFICATION"))
                        .status(),
                "el comentario es obligatorio");
        assertEquals(
                "IN_PROGRESS", get("/tasks/" + taskId, team.leader().token()).text("status"));

        requestModification(team, taskId);
        assertEquals(409, post("/requests", team.member().token(), body).status(), "ya hay una pendiente");
    }

    @Test
    void memberSeesOwnTasksInGroup() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var mine = get(
                "/tasks/group/" + team.groupId() + "/user/" + team.member().id(),
                team.member().token());
        assertTrue(containsId(mine.json(), taskId));
        var inProgress = get(
                "/tasks/group/" + team.groupId() + "?status=IN_PROGRESS",
                team.leader().token());
        assertTrue(containsId(inProgress.json(), taskId));
        assertFalse(containsId(
                get(
                                "/tasks/group/" + team.groupId() + "?status=DONE",
                                team.leader().token())
                        .json(),
                taskId));
    }
}
