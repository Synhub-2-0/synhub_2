package com.hub.synhub_backend.api;

import com.hub.synhub_backend.tasks.application.internal.schedulers.TaskStatusScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test void overdueTaskExpiresWithASingleRequestEvenWithConcurrentRuns() throws Exception {
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
        CompletableFuture.allOf(CompletableFuture.runAsync(run), CompletableFuture.runAsync(run)).get();
        scheduler.updateExpiredTasks();

        assertEquals("EXPIRED", get("/tasks/" + taskId, team.member().token()).text("status"));
        assertEquals(1, expiredRequests(taskId), "una sola request EXPIRED");
    }

    @Test void taskWithinItsDeadlineIsUntouched() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        scheduler.updateExpiredTasks();
        assertEquals("IN_PROGRESS", get("/tasks/" + taskId, team.member().token()).text("status"));
        assertEquals(0, expiredRequests(taskId));
    }
}
