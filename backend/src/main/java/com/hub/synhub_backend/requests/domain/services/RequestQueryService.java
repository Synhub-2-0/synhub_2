package com.hub.synhub_backend.requests.domain.services;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.queries.GetAllRequestsQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;

import java.util.List;
import java.util.Optional;

public interface RequestQueryService {
    List<Request> handle(GetAllRequestsQuery query);
    List<Request> handle(GetRequestsByTaskIdQuery query);
    Optional<Request> handle(GetRequestByIdQuery query);
    List<Request> handle(GetRequestsByGroupIdQuery query);
}
