package com.hub.synhub_backend.tasks;

import static org.junit.jupiter.api.Assertions.*;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskClassificationUpdate;
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

    /**
     * Tarea con createdAt/updatedAt fijados (en producción los pone @PrePersist). Si está IN_PROGRESS, lleva
     * {@code age} activa; en cualquier otro estado no tiene tiempo corriendo.
     */
    static Task task(TaskStatus status, Duration age) {
        var task = new Task(create("Implementar login"));
        ReflectionTestUtils.setField(task, "createdAt", NOW.minus(age));
        ReflectionTestUtils.setField(task, "updatedAt", NOW.minus(age));
        ReflectionTestUtils.setField(task, "activeSince", status == TaskStatus.IN_PROGRESS ? NOW.minus(age) : null);
        task.setStatus(status);
        return task;
    }

    private static double hours(Task task) {
        return Duration.ofMillis(task.getTimePassed()).toMinutes() / 60.0;
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
        assertNotNull(task.getActiveSince(), "el tiempo activo empieza al crearla");
    }

    @Test
    void completingMeasuresTheActiveTime() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(2));
        move(task, TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        var hours = Duration.ofMillis(task.getTimePassed()).toMinutes() / 60.0;
        assertTrue(hours >= 2 && hours < 2.1, "timePassed ≈ 2h, fue " + hours);
    }

    @Test
    void completingAfterReschedulingAddsTheActiveTimeToWhatWasAccumulated() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        ReflectionTestUtils.setField(task, "timesRearranged", 1);
        ReflectionTestUtils.setField(task, "timePassed", Duration.ofHours(5).toMillis());
        move(task, TaskStatus.COMPLETED);
        var hours = Duration.ofMillis(task.getTimePassed()).toMinutes() / 60.0;
        assertTrue(hours >= 6 && hours < 6.1, "timePassed ≈ 5h + 1h, fue " + hours);
    }

    @Test
    void timeOnHoldIsNotCounted() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(2));
        move(task, TaskStatus.ON_HOLD);
        assertNull(task.getActiveSince(), "en espera no corre el tiempo");
        assertTrue(hours(task) >= 2 && hours(task) < 2.1, "se guarda lo activo hasta la pausa: " + hours(task));

        // Estuvo 10 h en espera: al reanudar y completar enseguida, el total sigue siendo ≈2 h.
        ReflectionTestUtils.setField(task, "updatedAt", NOW.minusHours(10));
        move(task, TaskStatus.IN_PROGRESS);
        assertNotNull(task.getActiveSince());
        move(task, TaskStatus.COMPLETED);
        assertTrue(hours(task) >= 2 && hours(task) < 2.1, "el tiempo en espera no suma: " + hours(task));
    }

    @Test
    void editingTheTaskDoesNotLoseActiveTime() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(3));
        ReflectionTestUtils.setField(task, "timesRearranged", 1);
        ReflectionTestUtils.setField(task, "updatedAt", NOW); // una edición reciente mueve updatedAt
        move(task, TaskStatus.COMPLETED);
        assertTrue(hours(task) >= 3 && hours(task) < 3.1, "se mide desde que está activa, no desde updatedAt");
    }

    @Test
    void expiringStopsTheTimeAtTheDeadline() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(5));
        ReflectionTestUtils.setField(task, "dueDate", NOW.minusHours(1));
        move(task, TaskStatus.EXPIRED);
        assertNull(task.getActiveSince());
        assertTrue(hours(task) >= 4 && hours(task) < 4.1, "cuenta hasta la fecha límite (4 h): " + hours(task));
    }

    @Test
    void anExpiredTaskResumedStartsCountingAgain() {
        var task = task(TaskStatus.EXPIRED, Duration.ofHours(1));
        ReflectionTestUtils.setField(task, "timePassed", Duration.ofHours(4).toMillis());
        move(task, TaskStatus.IN_PROGRESS);
        assertNotNull(task.getActiveSince());
        assertEquals(Duration.ofHours(4).toMillis(), task.getTimePassed());
    }

    @Test
    void settlingAtTheDeadlineAddsTheTimeUpToIt() {
        var task = task(TaskStatus.EXPIRED, Duration.ofHours(1));
        ReflectionTestUtils.setField(task, "activeSince", NOW.minusHours(5));
        ReflectionTestUtils.setField(task, "dueDate", NOW.minusHours(2));
        task.settleActiveTime();
        assertNull(task.getActiveSince());
        assertEquals(Duration.ofHours(3).toMillis(), task.getTimePassed());
        task.settleActiveTime();
        assertEquals(Duration.ofHours(3).toMillis(), task.getTimePassed(), "es idempotente");
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
    void editingTheDueDateKeepsTheClockConsistent() {
        var paused = task(TaskStatus.ON_HOLD, Duration.ofDays(1));
        paused.updateTask(new UpdateTaskCommand(1L, 5L, null, null, NOW.plusDays(3), 2L));
        assertNotNull(paused.getActiveSince(), "reanudada por la edición: vuelve a correr");

        var active = task(TaskStatus.IN_PROGRESS, Duration.ofHours(2));
        active.updateTask(new UpdateTaskCommand(1L, 5L, null, null, NOW.minusHours(1), 2L));
        assertEquals(TaskStatus.EXPIRED, active.getStatus());
        assertNull(active.getActiveSince());
        assertTrue(hours(active) >= 1 && hours(active) < 1.1, "cuenta hasta la nueva fecha límite: " + hours(active));
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
    void updateWithoutClassificationKeepsTheSavedOne() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        task.updateTask(new UpdateTaskCommand(1L, 5L, "Nuevo título", null, null, 2L));
        assertEquals(3, task.getDifficulty());
        assertEquals("BACKEND", task.getContext());
        assertEquals(true, task.getAiAccepted());
    }

    @Test
    void updateReplacesTheClassification() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        task.updateTask(new UpdateTaskCommand(
                1L, 5L, null, null, null, 2L, new TaskClassificationUpdate(5, "CLOUD", "", " infra ", false)));
        assertEquals(5, task.getDifficulty());
        assertEquals("CLOUD", task.getContext());
        assertNull(task.getUrgency(), "un campo vacío se quita");
        assertEquals("infra", task.getLabels());
        assertEquals(false, task.getAiAccepted());
    }

    @Test
    void removingTheClassificationAlsoClearsTheAiMeasure() {
        var task = task(TaskStatus.IN_PROGRESS, Duration.ofHours(1));
        task.updateTask(new UpdateTaskCommand(
                1L, 5L, null, null, null, 2L, new TaskClassificationUpdate(null, null, "", " ", true)));
        assertNull(task.getDifficulty());
        assertNull(task.getContext());
        assertNull(task.getUrgency());
        assertNull(task.getLabels());
        assertNull(task.getAiAccepted());
    }

    @Test
    void theAiRationaleIsKeptOnlyWhileTheClassificationComesFromTheAi() {
        var fromAi = new Task(new CreateTaskCommand(
                1L, "Login", "d", NOW.plusDays(2), 2L, 10L, 3, "BACKEND", "HIGH", "api", true, " Toca el token. "));
        assertEquals("Toca el token.", fromAi.getAiRationale());

        fromAi.updateTask(new UpdateTaskCommand(
                1L,
                5L,
                null,
                null,
                null,
                2L,
                new TaskClassificationUpdate(4, "BACKEND", "HIGH", "api", false, "Toca el token.")));
        assertEquals("Toca el token.", fromAi.getAiRationale(), "editada a mano sigue siendo una sugerencia de la IA");

        fromAi.updateTask(new UpdateTaskCommand(
                1L, 5L, null, null, null, 2L, new TaskClassificationUpdate(4, "QA", null, null, null, "no aplica")));
        assertNull(fromAi.getAiRationale(), "clasificada a mano no tiene justificación de la IA");

        var byHand = new Task(new CreateTaskCommand(
                1L, "Login", "d", NOW.plusDays(2), 2L, 10L, 3, null, null, null, null, "no aplica"));
        assertNull(byHand.getAiRationale());
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
