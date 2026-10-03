package com.hub.synhub_backend.ai.interfaces.rest.resources;

import java.util.List;

public record TaskClassificationResource(
        List<String> labels,
        String urgency,
        int difficulty,
        String context,
        String rationale
) {
}
