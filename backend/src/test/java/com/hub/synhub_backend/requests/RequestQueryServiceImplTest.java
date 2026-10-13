package com.hub.synhub_backend.requests;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.requests.application.internal.queryservices.RequestQueryServiceImpl;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Bandeja del líder: GET /groups/{groupId}/requests. */
class RequestQueryServiceImplTest {

    private RequestRepository repo;
    private GroupsContextFacade groups;
    private TasksContextFacade tasks;
    private RequestQueryServiceImpl service;

    private static Request request(long id, String status, String type) {
        var r = mock(Request.class);
        when(r.getId()).thenReturn(id);
        when(r.getRequestStatus()).thenReturn(status);
        when(r.getRequestType()).thenReturn(type);
        return r;
    }

    @BeforeEach
    void setUp() {
        repo = mock(RequestRepository.class);
        groups = mock(GroupsContextFacade.class);
        tasks = mock(TasksContextFacade.class);
        service = new RequestQueryServiceImpl(repo, groups, tasks);
        when(groups.fetchLeaderIdByGroupId(1L)).thenReturn(Optional.of(7L));
    }

    // AUDIT PERF-01: el filtro y el orden (más nueva primero) los hace la consulta; aquí se valida qué se le pasa.
    @Test void withoutFiltersAsksForEverything() {
        var newestFirst = List.of(request(3, "PENDING", "SUBMISSION"), request(1, "APPROVED", "SUBMISSION"));
        when(repo.findByGroupFiltered(1L, null, null)).thenReturn(newestFirst);
        assertEquals(newestFirst, service.handle(new GetRequestsByGroupIdQuery(1L, null, null, 7L)));
    }

    @Test void filtersAreParsedIgnoringCase() {
        service.handle(new GetRequestsByGroupIdQuery(1L, "pending", "submission", 7L));
        verify(repo).findByGroupFiltered(1L, RequestStatus.PENDING, RequestType.SUBMISSION);
    }

    @Test void pagedVersionChecksTheLeaderToo() {
        var pageable = PageRequest.of(0, 2);
        when(repo.findByGroupFiltered(1L, RequestStatus.PENDING, null, pageable)).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.handle(new GetRequestsByGroupIdQuery(1L, "PENDING", null, 7L), pageable).getTotalElements());
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetRequestsByGroupIdQuery(1L, null, null, 8L), pageable));
    }

    @Test void onlyLeader() {
        var e = assertThrows(AccessDeniedException.class, () -> service.handle(new GetRequestsByGroupIdQuery(1L, null, null, 8L)));
        assertEquals("Only the leader of the group can list its requests", e.getMessage());
    }

    @Test void missingGroup() {
        var e = assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetRequestsByGroupIdQuery(9L, null, null, 7L)));
        assertEquals("Group with id 9 does not exist", e.getMessage());
    }

    @Test void unknownFilterIs400() {
        var e = assertThrows(IllegalArgumentException.class, () -> service.handle(new GetRequestsByGroupIdQuery(1L, "OPEN", null, 7L)));
        assertEquals("Unknown request status: OPEN", e.getMessage());
    }

    // ---- AUDIT SEC-01: requests de una tarea, solo para integrantes del grupo
    private Task taskInGroup(long groupId) {
        var task = mock(Task.class, RETURNS_DEEP_STUBS);
        when(task.getProfile().getGroup().getId()).thenReturn(groupId);
        return task;
    }

    @Test void requestsOfATaskForGroupMembers() {
        var task = taskInGroup(1L);
        when(tasks.getTaskById(5L)).thenReturn(Optional.of(task));
        when(groups.isMember(1L, 8L)).thenReturn(true);
        when(repo.findByTaskId(5L)).thenReturn(List.of());
        assertEquals(List.of(), service.handle(new GetRequestsByTaskIdQuery(5L, 8L)));
    }

    @Test void requestsOfATaskAreHiddenFromStrangers() {
        var task = taskInGroup(1L);
        when(tasks.getTaskById(5L)).thenReturn(Optional.of(task));
        when(groups.isMember(1L, 99L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetRequestsByTaskIdQuery(5L, 99L)));
        verify(repo, never()).findByTaskId(5L);
    }

    @Test void requestsOfUnknownTaskIs404() {
        when(tasks.getTaskById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetRequestsByTaskIdQuery(5L, 8L)));
    }

    @Test void requestByIdIsHiddenFromStrangers() {
        var request = mock(Request.class, RETURNS_DEEP_STUBS);
        when(request.getTask().getProfile().getGroup().getId()).thenReturn(1L);
        when(repo.findById(3L)).thenReturn(Optional.of(request));
        when(groups.isMember(1L, 8L)).thenReturn(true);
        assertSame(request, service.handle(new GetRequestByIdQuery(3L, 8L)).orElseThrow());
        assertThrows(AccessDeniedException.class, () -> service.handle(new GetRequestByIdQuery(3L, 99L)));
    }
}
