package com.hub.synhub_backend.tasks;

import com.hub.synhub_backend.tasks.aplication.internal.queryservices.TaskQueryServiceImpl;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Consultas de tareas. */
class TaskQueryServiceImplTest {

    private TaskRepository repo;
    private TaskQueryServiceImpl service;
    private final Task task = new Task();

    @BeforeEach
    void setUp() {
        repo = mock(TaskRepository.class);
        service = new TaskQueryServiceImpl(repo);
    }

    @Test void byGroupWithoutStatusReturnsAll() {
        when(repo.findByProfile_Group_Id(10L)).thenReturn(List.of(task));
        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndStatus(10L, null)));
        verify(repo, never()).findByProfile_Group_IdAndStatus(any(), any());
    }

    @Test void byGroupWithStatusFilters() {
        when(repo.findByProfile_Group_IdAndStatus(10L, TaskStatus.DONE)).thenReturn(List.of(task));
        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndStatus(10L, TaskStatus.DONE)));
    }

    @Test void byGroupAndUserByIdAndByStatusName() {
        when(repo.findByProfile_Group_IdAndProfile_User_Id(10L, 2L)).thenReturn(List.of(task));
        when(repo.findById(5L)).thenReturn(Optional.of(task));
        when(repo.findByStatus(TaskStatus.EXPIRED)).thenReturn(List.of(task));
        when(repo.findByProfile_User_Id(2L)).thenReturn(List.of(task));

        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndUserIdQuery(10L, 2L)));
        assertSame(task, service.handle(new GetTaskByIdQuery(5L)).orElseThrow());
        assertEquals(List.of(task), service.handle(new GetAllTaskByStatusQuery("EXPIRED")));
        assertEquals(List.of(task), service.handle(new GetAllTasksByUserId(2L)));
    }

    @Test void unknownStatusNameFails() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(new GetAllTaskByStatusQuery("ARCHIVED")));
    }
}
