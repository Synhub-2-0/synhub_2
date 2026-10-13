package com.hub.synhub_backend.requests.domain.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;

import java.util.List;
import java.util.Optional;

public interface RequestQueryService {
    List<Request> handle(GetRequestsByTaskIdQuery query);
    Optional<Request> handle(GetRequestByIdQuery query);
    List<Request> handle(GetRequestsByGroupIdQuery query);
    Page<Request> handle(GetRequestsByGroupIdQuery query, Pageable pageable);
}
