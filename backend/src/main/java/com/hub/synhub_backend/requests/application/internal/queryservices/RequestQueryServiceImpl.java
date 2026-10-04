package com.hub.synhub_backend.requests.application.internal.queryservices;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetAllRequestsQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RequestQueryServiceImpl implements RequestQueryService {

    private final RequestRepository requestRepository;

    public RequestQueryServiceImpl(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
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
        // fromString valida los filtros (valor desconocido -> 400)
        var status = query.status() == null ? null : RequestStatus.fromString(query.status()).name();
        var type = query.type() == null ? null : RequestType.fromString(query.type()).name();
        return this.requestRepository.findByTask_Profile_Group_Id(query.groupId()).stream()
                .filter(r -> status == null || r.getRequestStatus().equals(status))
                .filter(r -> type == null || r.getRequestType().equals(type))
                .toList();
    }
}
