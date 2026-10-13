package com.hub.synhub_backend.tasks;

import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskStatusScheduler;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Vencimiento automático de tareas. */
class TaskStatusSchedulerTest {

    private TaskRepository tasks;
    private RequestCommandService requests;
    private TaskStatusScheduler scheduler;
    private Task overdue;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        requests = mock(RequestCommandService.class);
        scheduler = new TaskStatusScheduler(tasks, requests);
        overdue = new Task();
        ReflectionTestUtils.setField(overdue, "id", 7L);
    }

    @Test void overdueTasksExpireAndGenerateASystemRequest() {
        when(tasks.findAllByStatusAndDueDateBefore(eq(TaskStatus.IN_PROGRESS), any(OffsetDateTime.class))).thenReturn(List.of(overdue));

        scheduler.updateExpiredTasks();

        assertEquals(TaskStatus.EXPIRED, overdue.getStatus());
        verify(tasks).save(overdue);
        var request = ArgumentCaptor.forClass(CreateRequestCommand.class);
        verify(requests).handle(request.capture());
        assertEquals("EXPIRED", request.getValue().requestType());
        assertEquals(7L, request.getValue().taskId());
        assertNull(request.getValue().requesterId(), "la genera el sistema, sin solicitante");
    }

    @Test void nothingHappensWithoutOverdueTasks() {
        when(tasks.findAllByStatusAndDueDateBefore(eq(TaskStatus.IN_PROGRESS), any(OffsetDateTime.class))).thenReturn(List.of());
        scheduler.updateExpiredTasks();
        verifyNoInteractions(requests);
        verify(tasks, never()).save(any());
    }

    @Disabled("AUDIT BUG-09: dos instancias que leen la misma tarea vencida crean dos requests EXPIRED. "
            + "Ajustar el doble del repositorio al mecanismo elegido (ShedLock o UPDATE condicional).")
    @Test void concurrentRunsDoNotDuplicateExpiredRequests() {
        // Simula dos instancias que leyeron la tarea antes de que la otra la marcara EXPIRED.
        when(tasks.findAllByStatusAndDueDateBefore(eq(TaskStatus.IN_PROGRESS), any(OffsetDateTime.class)))
                .thenAnswer(inv -> { var stale = new Task(); ReflectionTestUtils.setField(stale, "id", 7L); return List.of(stale); });
        var other = new TaskStatusScheduler(tasks, requests);

        scheduler.updateExpiredTasks();
        other.updateExpiredTasks();

        verify(requests, times(1)).handle(any(CreateRequestCommand.class));
    }
}
