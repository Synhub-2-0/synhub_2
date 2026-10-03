package com.hub.synhub_backend.ai.domain.model.queries;

import java.time.OffsetDateTime;

public record ClassifyTaskQuery(String title, String description, OffsetDateTime dueDate) {
}
