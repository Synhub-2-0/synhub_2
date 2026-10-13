package com.hub.synhub_backend.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.tasks.application.internal.schedulers.TaskStatusScheduler;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

/** Vencimiento automático de tareas. La concurrencia real contra PostgreSQL está en api/TaskExpirationApiTests. */
class TaskStatusSchedulerTest {

    private TaskRepository tasks;
    private RequestCommandService requests;
    private TaskStatusScheduler scheduler;
    private Task overdue;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        requests = mock(RequestCommandService.class);
        scheduler = new TaskStatusScheduler(tasks, requests, TransactionOperations.withoutTransaction());
        overdue = new Task();
        ReflectionTestUtils.setField(overdue, "id", 7L);
        when(tasks.findAllByStatusAndDueDateBefore(eq(TaskStatus.IN_PROGRESS), any(OffsetDateTime.class)))
                .thenReturn(List.of(overdue));
    }

    @Test
    void overdueTasksExpireAndGenerateASystemRequest() {
        when(tasks.markExpiredIfInProgress(eq(7L), any(OffsetDateTime.class))).thenReturn(1);

        scheduler.updateExpiredTasks();

        var request = ArgumentCaptor.forClass(CreateRequestCommand.class);
        verify(requests).handle(request.capture());
        assertEquals("EXPIRED", request.getValue().requestType());
        assertEquals(7L, request.getValue().taskId());
        assertNull(request.getValue().requesterId(), "la genera el sistema, sin solicitante");
    }

    @Test
    void nothingHappensWithoutOverdueTasks() {
        when(tasks.findAllByStatusAndDueDateBefore(eq(TaskStatus.IN_PROGRESS), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        scheduler.updateExpiredTasks();
        verifyNoInteractions(requests);
        verify(tasks, never()).markExpiredIfInProgress(any(), any());
    }

    // AUDIT BUG-09 (resuelto): la instancia que no cambió la fila no crea otra request
    @Test
    void concurrentRunsDoNotDuplicateExpiredRequests() {
        // Dos instancias leyeron la tarea vencida; el UPDATE condicional solo afecta la fila una vez.
        when(tasks.markExpiredIfInProgress(eq(7L), any(OffsetDateTime.class))).thenReturn(1, 0);
        var other = new TaskStatusScheduler(tasks, requests, TransactionOperations.withoutTransaction());

        scheduler.updateExpiredTasks();
        other.updateExpiredTasks();

        verify(requests, times(1)).handle(any(CreateRequestCommand.class));
    }
}
