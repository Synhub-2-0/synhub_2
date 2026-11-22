package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import com.hub.synhub_backend.tasks.application.internal.schedulers.TaskStatusScheduler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Borrado de tareas y grupos con datos relacionados. */
class CascadeDeleteApiTests extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TaskStatusScheduler scheduler;

    /** Una tarea con una validación de cada tipo y una ya revisada: devuelve los ids de las cuatro tareas y sus solicitudes. */
    private record Validations(List<Long> taskIds, List<Long> requestIds) {}

    private Validations validationsOfEveryType(Team team) {
        var leader = team.leader();
        var member = team.member();
        // entrega pendiente (la tarea pasa a COMPLETED)
        var submitted = createTask(leader, team.groupId(), member);
        var submission = submit(member, submitted);
        // modificación pendiente (la tarea queda en espera)
        var modified = createTask(leader, team.groupId(), member);
        var modification = post(
                        "/requests",
                        member.token(),
                        Map.of("taskId", modified, "description", "Cambiar", "requestType", "MODIFICATION"))
                .id();
        // vencida: el scheduler crea su solicitud EXPIRED
        var expired = createTask(leader, team.groupId(), member);
        jdbc.update("update tasks set due_date = now() - interval '1 hour' where id = ?", expired);
        scheduler.updateExpiredTasks();
        long expiredRequest = jdbc.queryForObject(
                "select id from requests where task_id = ? and request_type = 'EXPIRED'", Long.class, expired);
        // entrega ya rechazada (la tarea vuelve a IN_PROGRESS)
        var rejected = createTask(leader, team.groupId(), member);
        var rejection = submit(member, rejected);
        assertEquals(
                200,
                put("/requests/" + rejection + "/status/REJECTED", leader.token(), null)
                        .status());
        return new Validations(
                List.of(submitted, modified, expired, rejected),
                List.of(submission, modification, expiredRequest, rejection));
    }

    private int liveRequests(List<Long> ids) {
        return jdbc.queryForObject(
                "select count(*) from requests where id in ("
                        + String.join(",", ids.stream().map(String::valueOf).toList()) + ") and deleted_at is null",
                Integer.class);
    }

    @Test
    void deletingATaskDeletesItsValidationsOfEveryType() {
        var team = team();
        var v = validationsOfEveryType(team);
        assertEquals(
                3,
                get(
                                "/requests/group?groupId=" + team.groupId() + "&status=PENDING",
                                team.leader().token())
                        .json()
                        .size(),
                "entrega, modificación y vencida están pendientes en la bandeja");
        assertEquals(4, liveRequests(v.requestIds()));

        for (var taskId : v.taskIds()) {
            assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status(), "tarea " + taskId);
        }

        assertEquals(0, liveRequests(v.requestIds()), "ninguna validación sobrevive a su tarea");
        for (var requestId : v.requestIds()) {
            assertEquals(
                    404, get("/requests/" + requestId, team.leader().token()).status());
        }
        assertEquals(
                0,
                get("/requests/group?groupId=" + team.groupId(), team.leader().token())
                        .json()
                        .size(),
                "la bandeja del líder queda vacía en cualquier estado");
    }

    @Test
    void whenAMemberLeavesTheValidationsOfTheirTasksGoWithThem() {
        var team = team();
        var v = validationsOfEveryType(team);

        assertEquals(
                204,
                delete(
                                "/groups/" + team.groupId() + "/members/"
                                        + team.member().id(),
                                team.member().token())
                        .status());

        assertEquals(0, liveRequests(v.requestIds()));
        assertEquals(
                0,
                get("/requests/group?groupId=" + team.groupId(), team.leader().token())
                        .json()
                        .size());
    }

    @Test
    void deletingTheGroupDeletesAllItsValidations() {
        var team = team();
        var v = validationsOfEveryType(team);

        assertEquals(
                204, delete("/groups/" + team.groupId(), team.leader().token()).status());

        assertEquals(0, liveRequests(v.requestIds()));
    }

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
