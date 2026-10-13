package com.hub.synhub_backend.requests;

import com.hub.synhub_backend.requests.application.internal.commandservices.RequestCommandServiceImpl;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteAllRequestsByTaskIdCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Value objects y agregado Request, y borrado de requests.
 * Los permisos del borrado (AUDIT SEC-02) se prueban por HTTP en api/AuthorizationApiTests,
 * porque la corrección cambia la firma de DeleteRequestCommand.
 */
class RequestDomainAndDeleteTest {

    private RequestRepository repo;
    private TasksContextFacade tasks;
    private RequestCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(RequestRepository.class);
        tasks = mock(TasksContextFacade.class);
        service = new RequestCommandServiceImpl(repo, tasks);
    }

    // ---- Value objects
    @Test void typesAndStatusesParseIgnoringCase() {
        assertEquals(RequestType.SUBMISSION, RequestType.fromString("submission"));
        assertEquals(RequestType.MODIFICATION, RequestType.fromString("Modification"));
        assertEquals(RequestStatus.APPROVED, RequestStatus.fromString("approved"));
        assertThrows(IllegalArgumentException.class, () -> RequestType.fromString("DELETION"));
        assertThrows(IllegalArgumentException.class, () -> RequestStatus.fromString("CANCELLED"));
    }

    // ---- Agregado
    @Test void newRequestIsPending() {
        var request = new Request(new CreateRequestCommand("Listo", "submission", 1L));
        assertEquals("PENDING", request.getRequestStatus());
        assertEquals("SUBMISSION", request.getRequestType());
        assertEquals("Listo", request.getDescription());
    }

    @Test void statusCanBeUpdated() {
        var request = new Request(new CreateRequestCommand("Listo", "SUBMISSION", 1L));
        request.updateRequestStatus("rejected");
        assertEquals("REJECTED", request.getRequestStatus());
    }

    // ---- Borrado
    @Test void deletingUnknownRequestIsABadRequest() {
        when(repo.existsById(9L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.handle(new DeleteRequestCommand(9L)));
        verify(repo, never()).deleteById(any());
    }

    @Test void deletesExistingRequest() {
        when(repo.existsById(5L)).thenReturn(true);
        service.handle(new DeleteRequestCommand(5L));
        verify(repo).deleteById(5L);
    }

    @Test void deletesAllRequestsOfATask() {
        when(tasks.getTaskById(1L)).thenReturn(Optional.of(new Task()));
        service.handle(new DeleteAllRequestsByTaskIdCommand(1L));
        verify(repo).deleteByTaskId(1L);
    }

    @Test void deletingRequestsOfUnknownTaskFails() {
        when(tasks.getTaskById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.handle(new DeleteAllRequestsByTaskIdCommand(1L)));
        verify(repo, never()).deleteByTaskId(any());
    }
}
