package com.hub.synhub_backend.ai.domain.services;

import com.hub.synhub_backend.ai.domain.model.queries.GenerateGroupReportQuery;

import java.util.Optional;

public interface GroupReportQueryService {
    Optional<String> handle(GenerateGroupReportQuery query);
}
