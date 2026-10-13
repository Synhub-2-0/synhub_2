package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Borrado de tareas y grupos con datos relacionados. */
class CascadeDeleteApiTests extends ApiTestSupport {

    @Test void emptyGroupIsDeleted() {
        var leader = account("lider");
        var groupId = createGroup(leader);
        assertEquals(204, delete("/groups/" + groupId, leader.token()).status());
        assertEquals(404, get("/groups/" + groupId, leader.token()).status());
    }

    @Test void taskWithoutRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status());
    }

    @Disabled("AUDIT BUG-01: borrar una tarea con requests viola la FK requests.task_id (500)")
    @Test void taskWithRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status());
    }

    @Disabled("AUDIT BUG-02: borrar un grupo con tareas y requests falla (500)")
    @Test void groupWithTasksAndRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        createTask(team.leader(), team.groupId(), team.member());
        assertEquals(204, delete("/groups/" + team.groupId(), team.leader().token()).status());
        assertEquals(404, get("/groups/" + team.groupId(), team.leader().token()).status());
    }
}
