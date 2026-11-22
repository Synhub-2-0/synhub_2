package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import com.hub.synhub_backend.tasks.application.internal.schedulers.TaskStatusScheduler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Vencimiento automático contra PostgreSQL real (AUDIT BUG-09). */
class TaskExpirationApiTests extends ApiTestSupport {

    @Autowired
    private TaskStatusScheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    private int expiredRequests(long taskId) {
        return jdbc.queryForObject(
                "select count(*) from requests where task_id = ? and request_type = 'EXPIRED'", Integer.class, taskId);
    }

    @Test
    void overdueTaskExpiresWithASingleRequestEvenWithConcurrentRuns() throws Exception {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        jdbc.update("update tasks set due_date = now() - interval '1 hour' where id = ?", taskId);

        // Dos "instancias" corren a la vez sobre la misma tarea vencida.
        var barrier = new CyclicBarrier(2);
        Runnable run = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            scheduler.updateExpiredTasks();
        };
        CompletableFuture.allOf(CompletableFuture.runAsync(run), CompletableFuture.runAsync(run))
                .get();
        scheduler.updateExpiredTasks();

        assertEquals("EXPIRED", get("/tasks/" + taskId, team.member().token()).text("status"));
        assertEquals(1, expiredRequests(taskId), "una sola request EXPIRED");
    }

    @Test
    void anExpirationRequestCannotBeRejectedOnlyClosedByResuming() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        jdbc.update("update tasks set due_date = now() - interval '1 hour' where id = ?", taskId);
        scheduler.updateExpiredTasks();
        long requestId = jdbc.queryForObject(
                "select id from requests where task_id = ? and request_type = 'EXPIRED'", Long.class, taskId);

        assertEquals(
                409,
                put("/requests/" + requestId + "/status/REJECTED", team.leader().token(), null)
                        .status());
        assertEquals(
                "PENDING", get("/requests/" + requestId, team.leader().token()).text("requestStatus"));
        assertEquals(
                200,
                put("/requests/" + requestId + "/status/APPROVED", team.leader().token(), null)
                        .status());
    }

    @Test
    void resumingAnExpiredTaskByAnyRouteClosesItsExpirationNotice() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        jdbc.update("update tasks set due_date = now() - interval '1 hour' where id = ?", taskId);
        scheduler.updateExpiredTasks();
        long requestId = jdbc.queryForObject(
                "select id from requests where task_id = ? and request_type = 'EXPIRED'", Long.class, taskId);
        assertEquals(
                "PENDING", get("/requests/" + requestId, team.leader().token()).text("requestStatus"));

        // El líder la reanuda desde el tablero (estado) con una fecha nueva
        var newDate = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusDays(2);
        var edited = put(
                "/tasks/" + taskId,
                team.leader().token(),
                java.util.Map.of("userId", team.member().id(), "dueDate", newDate.toString()));
        assertEquals(200, edited.status(), edited.raw());
        assertEquals("EXPIRED", edited.text("status"), "cambiar la fecha sola no la reanuda");
        var resumed = put(
                "/tasks/" + taskId + "/status?status=IN_PROGRESS", team.leader().token(), null);

        assertEquals(200, resumed.status(), resumed.raw());
        assertEquals("IN_PROGRESS", resumed.text("status"));
        assertEquals(
                "APPROVED",
                get("/requests/" + requestId, team.leader().token()).text("requestStatus"),
                "el aviso de vencimiento se cierra solo");
        assertEquals(
                0,
                get(
                                "/requests/group?groupId=" + team.groupId() + "&status=PENDING",
                                team.leader().token())
                        .json()
                        .size());
    }

    @Test
    void anOverdueTaskStillExpiresInALockedGroup() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        jdbc.update("update groups set locked = true where id = ?", team.groupId());
        jdbc.update("update tasks set due_date = now() - interval '1 hour' where id = ?", taskId);

        scheduler.updateExpiredTasks();

        assertEquals("EXPIRED", get("/tasks/" + taskId, team.member().token()).text("status"));
        assertEquals(1, expiredRequests(taskId), "el tiempo pasa aunque el grupo esté en solo lectura");
    }

    @Test
    void taskWithinItsDeadlineIsUntouched() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        scheduler.updateExpiredTasks();
        assertEquals(
                "IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));
        assertEquals(0, expiredRequests(taskId));
    }
}
