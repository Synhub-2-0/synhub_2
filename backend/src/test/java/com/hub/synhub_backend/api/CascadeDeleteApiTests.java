package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Borrado de tareas y grupos con datos relacionados. */
class CascadeDeleteApiTests extends ApiTestSupport {

    @Test
    void emptyGroupIsDeleted() {
        var leader = account("lider");
        var groupId = createGroup(leader);
        assertEquals(204, delete("/groups/" + groupId, leader.token()).status());
        assertEquals(404, get("/groups/" + groupId, leader.token()).status());
    }

    @Test
    void taskWithoutRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status());
    }

    // AUDIT BUG-01 (resuelto)
    @Test
    void taskWithRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status());
        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status());
    }

    // AUDIT BUG-02 (resuelto)
    @Test
    void groupWithTasksAndRequestsIsDeleted() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), taskId);
        createTask(team.leader(), team.groupId(), team.member());
        assertEquals(
                204, delete("/groups/" + team.groupId(), team.leader().token()).status());
        assertEquals(
                404, get("/groups/" + team.groupId(), team.leader().token()).status());
    }
}
