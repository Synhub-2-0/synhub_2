package com.hub.synhub_backend.requests.application.internal.queryservices;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetAllRequestsQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RequestQueryServiceImpl implements RequestQueryService {

    private final RequestRepository requestRepository;
    private final GroupsContextFacade groupsContextFacade;
    private final TasksContextFacade tasksContextFacade;

    public RequestQueryServiceImpl(RequestRepository requestRepository, GroupsContextFacade groupsContextFacade,
                                   TasksContextFacade tasksContextFacade) {
        this.requestRepository = requestRepository;
        this.groupsContextFacade = groupsContextFacade;
        this.tasksContextFacade = tasksContextFacade;
    }

    private void requireMember(Long groupId, Long requesterId) {
        if (!groupsContextFacade.isMember(groupId, requesterId))
            throw new AccessDeniedException("Only members of the group can see the requests of its tasks");
    }

    @Override
    public List<Request> handle(GetAllRequestsQuery query) {
        return requestRepository.findAll();
    }

    @Override
    public List<Request> handle(GetRequestsByTaskIdQuery query) {
        var task = tasksContextFacade.getTaskById(query.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + query.taskId() + " does not exist"));
        requireMember(task.getProfile().getGroup().getId(), query.requesterId());
        return requestRepository.findByTaskId(query.taskId());
    }

    @Override
    public Optional<Request> handle(GetRequestByIdQuery query) {
        var request = this.requestRepository.findById(query.requestId());
        request.ifPresent(found -> requireMember(found.getTask().getProfile().getGroup().getId(), query.requesterId()));
        return request;
    }

    @Override
    public List<Request> handle(GetRequestsByGroupIdQuery query) {
        requireGroupLeader(query);
        return this.requestRepository.findByGroupFiltered(query.groupId(), statusOf(query), typeOf(query));
    }

    @Override
    public Page<Request> handle(GetRequestsByGroupIdQuery query, Pageable pageable) {
        requireGroupLeader(query);
        return this.requestRepository.findByGroupFiltered(query.groupId(), statusOf(query), typeOf(query), pageable);
    }

    private void requireGroupLeader(GetRequestsByGroupIdQuery query) {
        var leaderId = groupsContextFacade.fetchLeaderIdByGroupId(query.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + query.groupId() + " does not exist"));
        if (!leaderId.equals(query.requesterId()))
            throw new AccessDeniedException("Only the leader of the group can list its requests");
    }

    // fromString valida los filtros: un valor desconocido responde 400 ("Unknown request status: X").
    private static RequestStatus statusOf(GetRequestsByGroupIdQuery query) {
        return query.status() == null ? null : RequestStatus.fromString(query.status());
    }

    private static RequestType typeOf(GetRequestsByGroupIdQuery query) {
        return query.type() == null ? null : RequestType.fromString(query.type());
    }
}
