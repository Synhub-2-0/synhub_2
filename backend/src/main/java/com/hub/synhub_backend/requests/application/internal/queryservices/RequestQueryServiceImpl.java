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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RequestQueryServiceImpl implements RequestQueryService {

    private final RequestRepository requestRepository;
    private final GroupsContextFacade groupsContextFacade;

    public RequestQueryServiceImpl(RequestRepository requestRepository, GroupsContextFacade groupsContextFacade) {
        this.requestRepository = requestRepository;
        this.groupsContextFacade = groupsContextFacade;
    }

    @Override
    public List<Request> handle(GetAllRequestsQuery query) {
        return requestRepository.findAll();
    }

    @Override
    public List<Request> handle(GetRequestsByTaskIdQuery query) {
        return requestRepository.findByTaskId(query.taskId());
    }

    @Override
    public Optional<Request> handle(GetRequestByIdQuery query) {
        return this.requestRepository.findById(query.requestId());
    }

    @Override
    public List<Request> handle(GetRequestsByGroupIdQuery query) {
        var leaderId = groupsContextFacade.fetchLeaderIdByGroupId(query.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + query.groupId() + " does not exist"));
        if (!leaderId.equals(query.requesterId()))
            throw new AccessDeniedException("Only the leader of the group can list its requests");

        // fromString valida los filtros: un valor desconocido responde 400 ("Unknown request status: X").
        var status = query.status() == null ? null : RequestStatus.fromString(query.status()).name();
        var type = query.type() == null ? null : RequestType.fromString(query.type()).name();
        return this.requestRepository.findByTask_Profile_Group_Id(query.groupId()).stream()
                .filter(r -> status == null || r.getRequestStatus().equals(status))
                .filter(r -> type == null || r.getRequestType().equals(type))
                .sorted(Comparator.comparing(Request::getId).reversed())
                .toList();
    }
}
