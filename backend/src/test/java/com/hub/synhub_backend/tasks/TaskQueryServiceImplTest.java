package com.hub.synhub_backend.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.tasks.application.internal.queryservices.TaskQueryServiceImpl;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** Consultas de tareas. */
class TaskQueryServiceImplTest {

    private static final long MEMBER = 2L, STRANGER = 99L;

    private TaskRepository repo;
    private GroupsContextFacade groups;
    private TaskQueryServiceImpl service;
    private final Task task = mock(Task.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    void setUp() {
        repo = mock(TaskRepository.class);
        groups = mock(GroupsContextFacade.class);
        service = new TaskQueryServiceImpl(repo, groups);
        when(groups.isMember(10L, MEMBER)).thenReturn(true);
        when(task.getProfile().getGroup().getId()).thenReturn(10L);
    }

    @Test
    void byGroupWithoutStatusReturnsAll() {
        when(repo.findByProfile_Group_Id(10L)).thenReturn(List.of(task));
        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndStatus(10L, null, MEMBER)));
        verify(repo, never()).findByProfile_Group_IdAndStatus(any(), any());
    }

    @Test
    void byGroupWithStatusFilters() {
        when(repo.findByProfile_Group_IdAndStatus(10L, TaskStatus.DONE)).thenReturn(List.of(task));
        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndStatus(10L, TaskStatus.DONE, MEMBER)));
    }

    @Test
    void byGroupAndUserByIdAndByStatusName() {
        when(repo.findByProfile_Group_IdAndProfile_User_Id(10L, 2L)).thenReturn(List.of(task));
        when(repo.findById(5L)).thenReturn(Optional.of(task));
        when(repo.findByProfile_User_IdOrderByDueDateAscIdAsc(2L)).thenReturn(List.of(task));

        assertEquals(List.of(task), service.handle(new GetTasksByGroupIdAndUserIdQuery(10L, 2L, MEMBER)));
        assertSame(task, service.handle(new GetTaskByIdQuery(5L, MEMBER)).orElseThrow());
        assertEquals(List.of(task), service.handle(new GetAllTasksByUserId(2L)));
    }

    // ---- AUDIT SEC-01: solo los integrantes del grupo ven sus tareas
    @Test
    void strangersCannotReadTasksOfAGroup() {
        when(repo.findById(5L)).thenReturn(Optional.of(task));
        assertThrows(
                AccessDeniedException.class, () -> service.handle(new GetTasksByGroupIdAndStatus(10L, null, STRANGER)));
        assertThrows(
                AccessDeniedException.class,
                () -> service.handle(new GetTasksByGroupIdAndUserIdQuery(10L, MEMBER, STRANGER)));
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetTaskByIdQuery(5L, STRANGER)));
        verify(repo, never()).findByProfile_Group_Id(10L);
    }

    @Test
    void unknownTaskIsEmptyNotForbidden() {
        when(repo.findById(6L)).thenReturn(Optional.empty());
        assertTrue(service.handle(new GetTaskByIdQuery(6L, STRANGER)).isEmpty());
    }
}
