package com.hub.synhub_backend.ai.domain.model.valueobjects;

import java.util.List;

public record TaskClassification(
        List<String> labels,
        String urgency,
        int difficulty,
        String context,
        String rationale
) {
}
