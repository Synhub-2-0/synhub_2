package com.hub.synhub_backend.requests.application.internal.queryservices;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetAllRequestsQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
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
}
