package com.hub.synhub_backend.requests;

import com.hub.synhub_backend.requests.application.internal.commandservices.RequestCommandServiceImpl;
import com.hub.synhub_backend.requests.domain.exceptions.RequestConflictException;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestCommandServiceImplTest {

    private static final long TASK = 10L, MEMBER = 2L, LEADER = 1L;

    private RequestRepository repo;
    private TasksContextFacade tasks;
    private RequestCommandServiceImpl service;
    private Task task;

    @BeforeEach
    void setUp() {
        repo = mock(RequestRepository.class);
        tasks = mock(TasksContextFacade.class);
        service = new RequestCommandServiceImpl(repo, tasks);
        task = mock(Task.class, RETURNS_DEEP_STUBS);
        when(task.getId()).thenReturn(TASK);
        when(task.getProfile().getUser().getId()).thenReturn(MEMBER);
        when(task.getProfile().getGroup().getLeader().getId()).thenReturn(LEADER);
        when(tasks.getTaskById(TASK)).thenReturn(Optional.of(task));
        when(repo.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateRequestCommand submission(Long requester) {
        return new CreateRequestCommand("Listo", "SUBMISSION", TASK, requester);
    }

    private Request pendingSubmission() {
        var request = new Request(new CreateRequestCommand("Listo", "SUBMISSION", TASK));
        request.setTask(task);
        when(repo.findById(5L)).thenReturn(Optional.of(request));
        return request;
    }

    @Test
    void memberSubmitsCompletedTask() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        service.handle(submission(MEMBER));
        verify(repo).save(any(Request.class));
    }

    @Test
    void onlyTheAssignedMemberCanSubmit() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        assertThrows(AccessDeniedException.class, () -> service.handle(submission(99L)));
    }

    @Test
    void submissionNeedsCompletedTask() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        assertThrows(RequestConflictException.class, () -> service.handle(submission(MEMBER)));
    }

    @Test
    void noSecondPendingSubmission() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        when(repo.existsByTask_IdAndRequestTypeAndRequestStatus(TASK, RequestType.SUBMISSION, RequestStatus.PENDING)).thenReturn(true);
        var e = assertThrows(RequestConflictException.class, () -> service.handle(submission(MEMBER)));
        assertTrue(e.getMessage().contains("pending submission")); // el front muestra un texto específico con esto
    }

    @Test
    void unknownTaskIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new CreateRequestCommand("x", "SUBMISSION", 404L, MEMBER)));
    }

    @Test
    void systemRequestsSkipUserRules() {
        when(task.getStatus()).thenReturn(TaskStatus.EXPIRED);
        service.handle(new CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", TASK)); // scheduler
        verify(repo).save(any(Request.class));
    }

    @Test
    void approvingMarksTaskDone() {
        pendingSubmission();
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        var result = service.handle(new UpdateRequestCommand(5L, "APPROVED", LEADER)).orElseThrow();
        assertEquals("APPROVED", result.getRequestStatus());
        verify(tasks).updateTaskStatus(LEADER, TASK, TaskStatus.DONE);
    }

    @Test
    void rejectingSendsTaskBackToInProgress() {
        pendingSubmission();
        service.handle(new UpdateRequestCommand(5L, "REJECTED", LEADER));
        verify(tasks).updateTaskStatus(LEADER, TASK, TaskStatus.IN_PROGRESS);
    }

    @Test
    void onlyTheLeaderReviews() {
        pendingSubmission();
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateRequestCommand(5L, "APPROVED", MEMBER)));
        verify(tasks, never()).updateTaskStatus(any(), any(), any());
    }

    @Test
    void cannotReviewTwice() {
        var request = pendingSubmission();
        request.updateRequestStatus("APPROVED");
        var e = assertThrows(RequestConflictException.class, () -> service.handle(new UpdateRequestCommand(5L, "REJECTED", LEADER)));
        assertTrue(e.getMessage().contains("already reviewed"));
    }

    @Test
    void cannotApproveIfTaskNoLongerCompleted() {
        pendingSubmission();
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        assertThrows(RequestConflictException.class, () -> service.handle(new UpdateRequestCommand(5L, "APPROVED", LEADER)));
        verify(tasks, never()).updateTaskStatus(any(), any(), any());
    }
}
