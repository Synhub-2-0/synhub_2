package com.hub.synhub_backend.metrics.domain.model.valueobjects;

import java.util.Map;

public record TaskOverview(
        String type,
        int value,
        Map<String, Integer> details
) {}
