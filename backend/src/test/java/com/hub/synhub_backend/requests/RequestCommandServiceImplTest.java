package com.hub.synhub_backend.requests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.groups.domain.exceptions.GroupLockedException;
import com.hub.synhub_backend.requests.application.internal.commandservices.RequestCommandServiceImpl;
import com.hub.synhub_backend.requests.domain.exceptions.ExpiredRequestCannotBeRejectedException;
import com.hub.synhub_backend.requests.domain.exceptions.ModificationResolvedByEditingException;
import com.hub.synhub_backend.requests.domain.exceptions.PendingRequestAlreadyExistsException;
import com.hub.synhub_backend.requests.domain.exceptions.RequestAlreadyReviewedException;
import com.hub.synhub_backend.requests.domain.exceptions.TaskWithIncorrectStatusException;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** Reglas del flujo de entrega y validación (plan de Alex). */
class RequestCommandServiceImplTest {

    private static final long TASK = 10L, MEMBER = 2L, LEADER = 1L, REQUEST = 5L;

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

    private Request pending(String type) {
        var request = new Request(new CreateRequestCommand("Listo", type, TASK));
        request.setTask(task);
        when(repo.findById(REQUEST)).thenReturn(Optional.of(request));
        return request;
    }

    private UpdateRequestCommand review(String status, Long requester) {
        return new UpdateRequestCommand(REQUEST, status, requester);
    }

    // ---- Crear
    @Test
    void memberSubmitsCompletedTask() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        service.handle(submission(MEMBER));
        verify(repo).save(any(Request.class));
    }

    @Test
    void leaderCannotSubmit() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        var e = assertThrows(AccessDeniedException.class, () -> service.handle(submission(LEADER)));
        assertEquals("Only the member assigned to the task can create requests for it", e.getMessage());
    }

    @Test
    void submittingAnInProgressTaskCompletesIt() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        service.handle(submission(MEMBER));
        verify(tasks).completeForSubmission(TASK);
        verify(repo).save(any(Request.class));
    }

    @Test
    void submittingACompletedTaskDoesNotCompleteItAgain() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        service.handle(submission(MEMBER));
        verify(tasks, never()).completeForSubmission(any());
    }

    @Test
    void submissionNeedsAnInProgressOrCompletedTask() {
        when(task.getStatus()).thenReturn(TaskStatus.ON_HOLD);
        var e = assertThrows(TaskWithIncorrectStatusException.class, () -> service.handle(submission(MEMBER)));
        assertEquals(
                "A submission can only be created for an IN_PROGRESS or COMPLETED task, the task is ON_HOLD",
                e.getMessage());
        verify(tasks, never()).completeForSubmission(any());
    }

    @Test
    void onlyOnePendingSubmission() {
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        when(repo.existsByTask_IdAndRequestStatus(TASK, RequestStatus.PENDING)).thenReturn(true);
        var e = assertThrows(PendingRequestAlreadyExistsException.class, () -> service.handle(submission(MEMBER)));
        assertEquals(
                "Task with id 10 already has a pending request. Wait for it to be resolved before creating a new one",
                e.getMessage());
    }

    @Test
    void memberRequestsAModificationAndTheTaskIsPutOnHold() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        service.handle(new CreateRequestCommand("Cambiar el alcance", "MODIFICATION", TASK, MEMBER));
        verify(tasks).putOnHoldForModification(TASK);
        verify(repo).save(any(Request.class));
    }

    @Test
    void aModificationNeedsAComment() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.handle(new CreateRequestCommand("   ", "MODIFICATION", TASK, MEMBER)));
        verify(tasks, never()).putOnHoldForModification(anyLong());
        verify(repo, never()).save(any(Request.class));
    }

    @Test
    void aModificationNeedsAnInProgressTask() {
        when(task.getStatus()).thenReturn(TaskStatus.ON_HOLD);
        assertThrows(
                TaskWithIncorrectStatusException.class,
                () -> service.handle(new CreateRequestCommand("Cambiar", "MODIFICATION", TASK, MEMBER)));
        verify(tasks, never()).putOnHoldForModification(anyLong());
    }

    @Test
    void theLeaderCannotRequestAModification() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        assertThrows(
                AccessDeniedException.class,
                () -> service.handle(new CreateRequestCommand("Cambiar", "MODIFICATION", TASK, LEADER)));
        verify(tasks, never()).putOnHoldForModification(anyLong());
    }

    // ---- Un grupo bloqueado es de solo lectura
    private void lockTheGroup() {
        var lockedGroup = task.getProfile().getGroup();
        doThrow(new GroupLockedException(10L)).when(lockedGroup).ensureNotLocked();
    }

    @Test
    void nothingIsSentInALockedGroup() {
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        lockTheGroup();

        assertThrows(GroupLockedException.class, () -> service.handle(submission(MEMBER)));
        assertThrows(
                GroupLockedException.class,
                () -> service.handle(new CreateRequestCommand("Cambiar", "MODIFICATION", TASK, MEMBER)));
        verify(tasks, never()).completeForSubmission(anyLong());
        verify(tasks, never()).putOnHoldForModification(anyLong());
        verify(repo, never()).save(any(Request.class));
    }

    @Test
    void nothingIsReviewedOrDeletedInALockedGroup() {
        var request = pending("SUBMISSION");
        lockTheGroup();

        assertThrows(GroupLockedException.class, () -> service.handle(review("APPROVED", LEADER)));
        assertThrows(GroupLockedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, LEADER)));
        assertEquals("PENDING", request.getRequestStatus());
        verify(repo, never()).delete(any(Request.class));
    }

    @Test
    void theSystemStillRecordsExpiredTasksInALockedGroup() {
        lockTheGroup();
        service.handle(new CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", TASK));
        verify(repo).save(any(Request.class));
    }

    @Test
    void permissionsAreCheckedBeforeTheLockInRequests() {
        pending("SUBMISSION");
        lockTheGroup();
        assertThrows(AccessDeniedException.class, () -> service.handle(review("APPROVED", MEMBER)));
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, 99L)));
    }

    @Test
    void usersCannotCreateExpired() {
        var e = assertThrows(
                IllegalArgumentException.class,
                () -> service.handle(new CreateRequestCommand("x", "EXPIRED", TASK, MEMBER)));
        assertEquals("Requests of type EXPIRED are generated by the system", e.getMessage());
    }

    @Test
    void invalidType() {
        var e = assertThrows(
                IllegalArgumentException.class,
                () -> service.handle(new CreateRequestCommand("x", "OTRO", TASK, MEMBER)));
        assertEquals("Invalid request type", e.getMessage());
    }

    @Test
    void unknownTask() {
        var e = assertThrows(
                ResourceNotFoundException.class,
                () -> service.handle(new CreateRequestCommand("x", "SUBMISSION", 404L, MEMBER)));
        assertEquals("Task with id 404 does not exist", e.getMessage());
    }

    @Test
    void systemExpiredRequestStillWorks() {
        when(task.getStatus()).thenReturn(TaskStatus.EXPIRED);
        service.handle(new CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", TASK)); // scheduler
        verify(repo).save(any(Request.class));
    }

    // ---- Revisar
    @Test
    void approveMovesTaskToDone() {
        pending("SUBMISSION");
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        assertEquals(
                "APPROVED",
                service.handle(review("APPROVED", LEADER)).orElseThrow().getRequestStatus());
        verify(tasks).applySubmissionReview(TASK, true);
    }

    @Test
    void rejectMovesTaskBackToInProgress() {
        pending("SUBMISSION");
        when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        service.handle(review("REJECTED", LEADER));
        verify(tasks).applySubmissionReview(TASK, false);
    }

    @Test
    void rejectWhenTaskNoLongerCompletedOnlyClosesRequest() {
        pending("SUBMISSION");
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        assertEquals(
                "REJECTED",
                service.handle(review("REJECTED", LEADER)).orElseThrow().getRequestStatus());
        verify(tasks, never()).applySubmissionReview(anyLong(), anyBoolean());
    }

    @Test
    void approveWhenTaskNoLongerCompletedIs409() {
        pending("SUBMISSION");
        when(task.getStatus()).thenReturn(TaskStatus.IN_PROGRESS);
        var e = assertThrows(TaskWithIncorrectStatusException.class, () -> service.handle(review("APPROVED", LEADER)));
        assertEquals("Only a COMPLETED task can be approved, the task is IN_PROGRESS", e.getMessage());
    }

    @Test
    void onlyLeaderReviews() {
        pending("SUBMISSION");
        var e = assertThrows(AccessDeniedException.class, () -> service.handle(review("APPROVED", MEMBER)));
        assertEquals("Only the leader of the group can review requests", e.getMessage());
    }

    @Test
    void reviewTwiceIs409() {
        pending("SUBMISSION").updateRequestStatus("REJECTED");
        var e = assertThrows(RequestAlreadyReviewedException.class, () -> service.handle(review("APPROVED", LEADER)));
        assertTrue(e.getMessage().endsWith("was already reviewed: REJECTED"));
    }

    @Test
    void reviewAsPendingIs400() {
        pending("SUBMISSION");
        var e = assertThrows(IllegalArgumentException.class, () -> service.handle(review("PENDING", LEADER)));
        assertEquals("A request can only be reviewed as APPROVED or REJECTED", e.getMessage());
    }

    @Test
    void missingRequestIs404() {
        when(repo.findById(REQUEST)).thenReturn(Optional.empty());
        var e = assertThrows(ResourceNotFoundException.class, () -> service.handle(review("APPROVED", LEADER)));
        assertEquals("Request with id 5 does not exist", e.getMessage());
    }

    @Test
    void aModificationIsApprovedByEditingTheTaskNotByReviewingIt() {
        var request = pending("MODIFICATION");
        assertThrows(ModificationResolvedByEditingException.class, () -> service.handle(review("APPROVED", LEADER)));
        assertEquals("PENDING", request.getRequestStatus());
        verify(repo, never()).save(any(Request.class));
        verify(tasks, never()).resumeAfterModificationDenied(anyLong());
        verify(tasks, never()).applySubmissionReview(anyLong(), anyBoolean());
    }

    @Test
    void anExpiredRequestCannotBeRejectedItIsClosedByResumingTheTask() {
        var request = pending("EXPIRED");

        assertThrows(ExpiredRequestCannotBeRejectedException.class, () -> service.handle(review("REJECTED", LEADER)));
        assertEquals("PENDING", request.getRequestStatus());
        verify(repo, never()).save(any(Request.class));

        // Reanudar la tarea cierra su aviso: la web la aprueba al terminar.
        assertEquals(
                "APPROVED",
                service.handle(review("APPROVED", LEADER)).orElseThrow().getRequestStatus());
        verify(tasks, never()).applySubmissionReview(anyLong(), anyBoolean());
    }

    @Test
    void theLeaderCanDenyAModificationAndTheTaskResumes() {
        var request = pending("MODIFICATION");

        var reviewed = service.handle(review("REJECTED", LEADER)).orElseThrow();

        assertEquals("REJECTED", reviewed.getRequestStatus());
        assertEquals("REJECTED", request.getRequestStatus());
        verify(tasks).resumeAfterModificationDenied(TASK);
        verify(tasks, never()).applySubmissionReview(anyLong(), anyBoolean());
        verify(repo).save(request);
    }

    @Test
    void aDeniedModificationCannotBeReviewedAgain() {
        var request = pending("MODIFICATION");
        request.updateRequestStatus("REJECTED");
        assertThrows(RequestAlreadyReviewedException.class, () -> service.handle(review("REJECTED", LEADER)));
        verify(tasks, never()).resumeAfterModificationDenied(anyLong());
    }

    @Test
    void onlyTheLeaderDeniesAModification() {
        pending("MODIFICATION");
        assertThrows(AccessDeniedException.class, () -> service.handle(review("REJECTED", MEMBER)));
        verify(tasks, never()).resumeAfterModificationDenied(anyLong());
    }

    @Test
    void onlyTheLeaderGetsToKnowAboutAModificationReview() {
        pending("MODIFICATION");
        assertThrows(AccessDeniedException.class, () -> service.handle(review("APPROVED", MEMBER)));
    }

    // ---- Borrar (AUDIT SEC-02)
    @Test
    void leaderDeletesAnyRequest() {
        var request = pending("EXPIRED");
        service.handle(new DeleteRequestCommand(REQUEST, LEADER));
        verify(repo).delete(request);
    }

    @Test
    void memberDeletesOwnPendingSubmission() {
        var request = pending("SUBMISSION");
        service.handle(new DeleteRequestCommand(REQUEST, MEMBER));
        verify(repo).delete(request);
    }

    @Test
    void aPendingModificationCannotBeDeletedByAnyone() {
        var request = pending("MODIFICATION");
        assertThrows(
                ModificationResolvedByEditingException.class,
                () -> service.handle(new DeleteRequestCommand(REQUEST, LEADER)));
        assertThrows(
                ModificationResolvedByEditingException.class,
                () -> service.handle(new DeleteRequestCommand(REQUEST, MEMBER)));
        verify(repo, never()).delete(any(Request.class));

        // Un ajeno recibe 403, sin enterarse de que es una modificación pendiente.
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, 99L)));

        // Resuelta al editar la tarea, el líder ya puede borrarla.
        request.updateRequestStatus("APPROVED");
        service.handle(new DeleteRequestCommand(REQUEST, LEADER));
        verify(repo).delete(request);
    }

    @Test
    void memberCannotDeleteReviewedOrSystemRequests() {
        var reviewed = pending("SUBMISSION");
        reviewed.updateRequestStatus("APPROVED");
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, MEMBER)));
        pending("EXPIRED");
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, MEMBER)));
        verify(repo, never()).delete(any(Request.class));
    }

    @Test
    void strangersCannotDeleteRequests() {
        pending("SUBMISSION");
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, 99L)));
        verify(repo, never()).delete(any(Request.class));
    }

    @Test
    void deletingAnUnknownRequestIs404() {
        when(repo.findById(REQUEST)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new DeleteRequestCommand(REQUEST, LEADER)));
    }
}
