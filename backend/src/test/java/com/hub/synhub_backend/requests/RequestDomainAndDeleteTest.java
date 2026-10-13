package com.hub.synhub_backend.requests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.requests.application.internal.commandservices.RequestCommandServiceImpl;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Value objects y agregado Request, y borrado de todas las requests de una tarea.
 * El borrado individual y sus permisos (AUDIT SEC-02) están en RequestCommandServiceImplTest.
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
    @Test
    void typesAndStatusesParseIgnoringCase() {
        assertEquals(RequestType.SUBMISSION, RequestType.fromString("submission"));
        assertEquals(RequestType.MODIFICATION, RequestType.fromString("Modification"));
        assertEquals(RequestStatus.APPROVED, RequestStatus.fromString("approved"));
        assertThrows(IllegalArgumentException.class, () -> RequestType.fromString("DELETION"));
        assertThrows(IllegalArgumentException.class, () -> RequestStatus.fromString("CANCELLED"));
    }

    // ---- Agregado
    @Test
    void newRequestIsPending() {
        var request = new Request(new CreateRequestCommand("Listo", "submission", 1L));
        assertEquals("PENDING", request.getRequestStatus());
        assertEquals("SUBMISSION", request.getRequestType());
        assertEquals("Listo", request.getDescription());
    }

    @Test
    void statusCanBeUpdated() {
        var request = new Request(new CreateRequestCommand("Listo", "SUBMISSION", 1L));
        request.updateRequestStatus("rejected");
        assertEquals("REJECTED", request.getRequestStatus());
    }
}
