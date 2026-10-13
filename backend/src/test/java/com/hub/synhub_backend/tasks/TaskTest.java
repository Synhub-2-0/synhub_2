package com.hub.synhub_backend.tasks;

import static org.junit.jupiter.api.Assertions.*;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Ciclo de estados y métricas del agregado Task. */
class TaskTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    static CreateTaskCommand create(String title) {
        return new CreateTaskCommand(
                1L, title, "Descripción", NOW.plusDays(2), 2L, 10L, 3, "BACKEND", "HIGH", "api, auth", true);
    }

    /** Tarea con createdAt/updatedAt fijados (en producción los pone @PrePersist). */
    static Task task(TaskStatus status, Duration age) {
        var task = new Task(create("Implementar login"));
        ReflectionTestUtils.setField(task, "createdAt", NOW.minus(age));
        ReflectionTestUtils.setField(task, "updatedAt", NOW.minus(age));
        task.setStatus(status);
        return task;
    }

    private static void move(Task task, TaskStatus to) {
        task.updateStatus(new UpdateTaskStatusCommand(1L, task.getId(), to));
    }

    @Test
    void newTaskStartsInProgressWithAiFields() {
        var task = new Task(create("Implementar login"));
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals("Implementar login", task.getTitle());
        assertEquals(3, task.getDifficulty());
        assertEquals("BACKEND", task.getContext());
        assertEquals("HIGH", task.getUrgency());
        assertEquals("api, auth", task.getLabels());
        assertTrue(task.getAiAccepted());
        assertEquals(0, task.getTimesRearranged());
        assertEquals(0L, task.getTimePassed());
    }

    @Test
    void completingMeasuresTimeSinceCreation() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(2));
        move(task, TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        var hours = Duration.ofMillis(task.getTimePassed()).toMinutes() / 60.0;
        assertTrue(hours >= 2 && hours < 2.1, "timePassed ≈ 2h, fue " + hours);
    }

    @Test
    void completingAfterReschedulingAddsTimeSinceLastUpdate() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        ReflectionTestUtils.setField(task, "timesRearranged", 1);
        ReflectionTestUtils.setField(task, "timePassed", Duration.ofHours(5).toMillis());
        move(task, TaskStatus.COMPLETED);
        var hours = Duration.ofMillis(task.getTimePassed()).toMinutes() / 60.0;
        assertTrue(hours >= 6 && hours < 6.1, "timePassed ≈ 5h + 1h, fue " + hours);
    }

    @Test
    void returningToInProgressCountsAsRescheduling() {
        for (var from : new TaskStatus[] {TaskStatus.COMPLETED, TaskStatus.ON_HOLD, TaskStatus.EXPIRED}) {
            var task = task(from, Duration.ofHours(1));
            move(task, TaskStatus.IN_PROGRESS);
            assertEquals(1, task.getTimesRearranged(), "desde " + from);
            assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        }
    }

    @Test
    void pausingDoesNotCountAsRescheduling() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        move(task, TaskStatus.ON_HOLD);
        assertEquals(TaskStatus.ON_HOLD, task.getStatus());
        assertEquals(0, task.getTimesRearranged());
    }

    @Test
    void pastDueDateExpiresTheTask() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofDays(3));
        task.updateTask(new UpdateTaskCommand(1L, 5L, null, null, NOW.minusDays(1), 2L));
        assertEquals(TaskStatus.EXPIRED, task.getStatus());
    }

    @Test
    void futureDueDateReactivatesAPausedTaskButNotAnExpiredOne() {
        var paused = task(TaskStatus.ON_HOLD, Duration.ofDays(1));
        paused.updateTask(new UpdateTaskCommand(1L, 5L, null, null, NOW.plusDays(3), 2L));
        assertEquals(TaskStatus.IN_PROGRESS, paused.getStatus());

        var expired = task(TaskStatus.EXPIRED, Duration.ofDays(5));
        expired.updateTask(new UpdateTaskCommand(1L, 5L, null, null, NOW.plusDays(3), 2L));
        assertEquals(TaskStatus.EXPIRED, expired.getStatus(), "una tarea vencida se reactiva solo cambiando su estado");
    }

    @Test
    void updateIgnoresEmptyFields() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        task.updateTask(new UpdateTaskCommand(1L, 5L, "", null, null, 2L));
        assertEquals("Implementar login", task.getTitle());
        assertEquals("Descripción", task.getDescription());
        task.updateTask(new UpdateTaskCommand(1L, 5L, "Nuevo título", "Nueva desc", null, 2L));
        assertEquals("Nuevo título", task.getTitle());
        assertEquals("Nueva desc", task.getDescription());
    }

    @Test
    void approvedSubmissionMarksDone() {
        var task = task(TaskStatus.COMPLETED, Duration.ofHours(1));
        task.applySubmissionReview(true);
        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @Test
    void rejectedSubmissionReopensAndCountsRescheduling() {
        var task = task(TaskStatus.COMPLETED, Duration.ofHours(1));
        task.applySubmissionReview(false);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(1, task.getTimesRearranged());
    }

    @Test
    void statusFromString() {
        assertEquals(TaskStatus.ON_HOLD, TaskStatus.fromString("on_hold"));
        assertThrows(IllegalArgumentException.class, () -> TaskStatus.fromString("ARCHIVED"));
    }

    // AUDIT BUG-03 (resuelto)
    @Test
    void titleIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new Task(create(null)));
        assertThrows(IllegalArgumentException.class, () -> new Task(create("  ")));
        var noDueDate = new CreateTaskCommand(1L, "Login", "d", null, 2L, 10L, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> new Task(noDueDate));
        var noDescription =
                new CreateTaskCommand(1L, "Login", null, NOW.plusDays(1), 2L, 10L, null, null, null, null, null);
        assertEquals("", new Task(noDescription).getDescription());
    }

    // AUDIT BUG-18
    @Test
    void onlyDoneTasksAreFinished() {
        var task = new Task();
        ReflectionTestUtils.setField(task, "id", 4L);
        assertDoesNotThrow(task::ensureNotFinished);
        task.setStatus(TaskStatus.DONE);
        var e = assertThrows(
                com.hub.synhub_backend.tasks.domain.exceptions.TaskFinishedException.class, task::ensureNotFinished);
        assertTrue(e.getMessage().contains("4"));
    }
}
