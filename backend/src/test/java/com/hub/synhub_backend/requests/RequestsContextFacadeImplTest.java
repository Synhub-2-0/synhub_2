package com.hub.synhub_backend.requests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hub.synhub_backend.requests.application.internal.acl.RequestsContextFacadeImpl;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ACL que requests ofrece a tasks: solicitudes de modificación pendientes de una tarea. */
class RequestsContextFacadeImplTest {

    private static final long TASK = 10L;

    private RequestRepository repo;
    private RequestsContextFacadeImpl facade;

    @BeforeEach
    void setUp() {
        repo = mock(RequestRepository.class);
        facade = new RequestsContextFacadeImpl(repo);
    }

    private static Request request(String type, String status) {
        var request = new Request(new CreateRequestCommand("Cambiar", type, TASK));
        request.updateRequestStatus(status);
        return request;
    }

    @Test
    void knowsWhetherATaskHasAPendingModification() {
        when(repo.existsByTask_IdAndRequestTypeAndRequestStatus(TASK, RequestType.MODIFICATION, RequestStatus.PENDING))
                .thenReturn(true);
        assertTrue(facade.hasPendingModification(TASK));
        assertFalse(facade.hasPendingModification(99L));
    }

    @Test
    void resolvingApprovesOnlyThePendingModification() {
        var modification = request("MODIFICATION", "PENDING");
        var submission = request("SUBMISSION", "PENDING");
        var oldModification = request("MODIFICATION", "REJECTED");
        when(repo.findByTaskId(TASK)).thenReturn(List.of(modification, submission, oldModification));

        assertTrue(facade.resolvePendingModification(TASK));

        assertEquals("APPROVED", modification.getRequestStatus());
        assertEquals("PENDING", submission.getRequestStatus());
        assertEquals("REJECTED", oldModification.getRequestStatus());
        verify(repo).saveAll(List.of(modification));
    }

    @Test
    void resolvingWithoutAPendingModificationDoesNothing() {
        when(repo.findByTaskId(TASK)).thenReturn(List.of(request("EXPIRED", "PENDING")));
        assertFalse(facade.resolvePendingModification(TASK));
        verify(repo, never()).saveAll(any());
    }
}
