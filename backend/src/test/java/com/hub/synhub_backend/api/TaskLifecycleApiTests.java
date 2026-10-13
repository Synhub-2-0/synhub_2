package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/** Flujos completos por HTTP: grupo, invitación, tarea, entrega y validación. */
class TaskLifecycleApiTests extends ApiTestSupport {

    private static boolean containsId(tools.jackson.databind.JsonNode array, long id) {
        return StreamSupport.stream(array.spliterator(), false).anyMatch(node -> node.get("id").asLong() == id);
    }

    @Test void invitationFlowAddsMember() {
        var leader = account("lider");
        var member = account("miembro");
        var groupId = createGroup(leader);
        assertTrue(containsId(get("/groups/user/role?groupRole=GROUP_LEADER", leader.token()).json(), groupId));

        var invitation = post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId));
        assertEquals(200, invitation.status());
        assertTrue(containsId(get("/invitations/user", member.token()).json(), invitation.id()));
        assertEquals(409, post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId)).status(),
                "invitación duplicada");

        assertEquals(200, post("/invitations/accept?invitationId=" + invitation.id(), member.token(), null).status());

        var members = get("/groups/" + groupId + "/members", leader.token());
        assertEquals(2, members.json().size());
        assertTrue(containsId(get("/groups/user/role?groupRole=GROUP_MEMBER", member.token()).json(), groupId));
        assertEquals(0, get("/invitations/user", member.token()).json().size());
        assertEquals(409, post("/invitations", leader.token(), Map.of("userId", member.id(), "groupId", groupId)).status(),
                "ya es miembro");
    }

    @Test void declinedInvitationDisappears() {
        var leader = account("lider");
        var guest = account("invitado");
        var groupId = createGroup(leader);
        var invitation = post("/invitations", leader.token(), Map.of("userId", guest.id(), "groupId", groupId));

        assertEquals(204, delete("/invitations/" + invitation.id(), guest.token()).status());
        assertEquals(0, get("/invitations/user", guest.token()).json().size());
        assertEquals(1, get("/groups/" + groupId + "/members", leader.token()).json().size());
    }

    @Test void approvedSubmissionMarksTaskDone() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals("IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));

        var requestId = submit(team.member(), taskId);
        assertEquals(409, post("/tasks/" + taskId + "/requests", team.member().token(),
                Map.of("description", "otra", "requestType", "SUBMISSION")).status(), "solo una entrega pendiente");

        var inbox = get("/groups/" + team.groupId() + "/requests?status=PENDING&type=SUBMISSION", team.leader().token());
        assertEquals(200, inbox.status());
        assertTrue(containsId(inbox.json(), requestId));

        var approved = put("/tasks/" + taskId + "/requests/" + requestId + "/status/APPROVED", team.leader().token(), null);
        assertEquals(200, approved.status(), approved.raw());
        assertEquals("APPROVED", approved.text("requestStatus"));
        assertEquals("DONE", get("/tasks/" + taskId, team.leader().token()).text("status"));
        assertEquals(409, put("/tasks/" + taskId + "/requests/" + requestId + "/status/REJECTED", team.leader().token(), null).status(),
                "una request se revisa una sola vez");
        assertFalse(containsId(get("/groups/" + team.groupId() + "/requests?status=PENDING", team.leader().token()).json(), requestId));
    }

    // AUDIT BUG-18: una tarea DONE es definitiva también para la API (la web ya lo impedía)
    @Test void doneTaskCannotBeEditedMovedOrDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        assertEquals(200, put("/tasks/" + taskId + "/requests/" + requestId + "/status/APPROVED", team.leader().token(), null).status());

        assertEquals(409, put("/tasks/" + taskId, team.leader().token(), Map.of("title", "Otra", "userId", team.member().id())).status());
        assertEquals(409, put("/tasks/" + taskId + "/status?status=IN_PROGRESS", team.member().token(), null).status());
        assertEquals(409, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(403, delete("/tasks/" + taskId, team.member().token()).status(), "los permisos se revisan primero");
        assertEquals("DONE", get("/tasks/" + taskId, team.leader().token()).text("status"));
        assertEquals(204, delete("/groups/" + team.groupId(), team.leader().token()).status(), "borrar el grupo sí borra sus tareas DONE");
    }

    @Test void rejectedSubmissionReopensTask() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);

        var rejected = put("/tasks/" + taskId + "/requests/" + requestId + "/status/REJECTED", team.leader().token(), null);

        assertEquals(200, rejected.status());
        assertEquals("IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));
    }

    @Test void taskCannotBeMarkedDoneDirectly() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(409, put("/tasks/" + taskId + "/status?status=DONE", team.leader().token(), null).status());
    }

    @Test void submissionRequiresACompletedTask() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var res = post("/tasks/" + taskId + "/requests", team.member().token(), Map.of("description", "x", "requestType", "SUBMISSION"));
        assertEquals(409, res.status());
    }

    @Test void memberSeesOwnTasksInGroup() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var mine = get("/tasks/group/" + team.groupId() + "/user/" + team.member().id(), team.member().token());
        assertTrue(containsId(mine.json(), taskId));
        var inProgress = get("/tasks/group/" + team.groupId() + "?status=IN_PROGRESS", team.leader().token());
        assertTrue(containsId(inProgress.json(), taskId));
        assertFalse(containsId(get("/tasks/group/" + team.groupId() + "?status=DONE", team.leader().token()).json(), taskId));
    }
}
