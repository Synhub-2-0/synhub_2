package com.hub.synhub_backend.ai.domain.services;

import com.hub.synhub_backend.ai.domain.model.queries.ClassifyTaskQuery;
import com.hub.synhub_backend.ai.domain.model.valueobjects.TaskClassification;

import java.util.Optional;

public interface TaskClassificationQueryService {
    Optional<TaskClassification> handle(ClassifyTaskQuery query);
}
