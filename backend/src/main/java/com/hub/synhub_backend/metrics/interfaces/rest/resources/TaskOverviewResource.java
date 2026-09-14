package com.hub.synhub_backend.metrics.interfaces.rest.resources;

import java.util.Map;

public record TaskOverviewResource(
        String type,
        int value,
        Map<String, Integer> details
) {}
