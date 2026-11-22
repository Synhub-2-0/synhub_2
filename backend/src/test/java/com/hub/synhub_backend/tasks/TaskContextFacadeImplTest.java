package com.hub.synhub_backend.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.application.internal.acl.TaskContextFacadeImpl;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTasksByUserIdAndGroupIdCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** ACL que tasks ofrece a requests y groups. */
class TaskContextFacadeImplTest {

    private TaskCommandService commands;
    private TaskQueryService queries;
    private TaskRepository repo;
    private TaskContextFacadeImpl facade;

    @BeforeEach
    void setUp() {
        commands = mock(TaskCommandService.class);
        queries = mock(TaskQueryService.class);
        repo = mock(TaskRepository.class);
        facade = new TaskContextFacadeImpl(commands, queries, repo);
        when(repo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getTaskById() {
        var task = new Task();
        when(repo.findById(1L)).thenReturn(Optional.of(task));
        assertSame(task, facade.getTaskById(1L).orElseThrow());
        assertTrue(facade.getTaskById(2L).isEmpty());
    }

    @Test
    void submissionReviewIsAppliedAndSaved() {
        var task = new Task();
        task.setStatus(TaskStatus.COMPLETED);
        when(repo.findById(1L)).thenReturn(Optional.of(task));

        assertEquals(TaskStatus.DONE, facade.applySubmissionReview(1L, true).getStatus());
        verify(repo).save(task);
    }

    @Test
    void aModificationRequestPutsTheTaskOnHoldAndStopsItsClock() {
        var task = new Task();
        ReflectionTestUtils.setField(
                task, "dueDate", OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        ReflectionTestUtils.setField(
                task, "activeSince", OffsetDateTime.now(ZoneOffset.UTC).minusHours(2));
        when(repo.findById(1L)).thenReturn(Optional.of(task));

        assertEquals(TaskStatus.ON_HOLD, facade.putOnHoldForModification(1L).getStatus());
        assertNull(task.getActiveSince());
        assertTrue(task.getTimePassed() >= Duration.ofHours(2).toMillis());
        verify(repo).save(task);
    }

    @Test
    void putOnHoldOfUnknownTaskIs404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> facade.putOnHoldForModification(9L));
    }

    @Test
    void reviewOfUnknownTaskIs404() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> facade.applySubmissionReview(9L, true));
    }

    @Test
    void deleteTasksOfMemberDelegatesToCommandService() {
        facade.deleteTasksByUserIdAndGroupId(2L, 10L);
        verify(commands).handle(new DeleteTasksByUserIdAndGroupIdCommand(2L, 10L));
    }
}
