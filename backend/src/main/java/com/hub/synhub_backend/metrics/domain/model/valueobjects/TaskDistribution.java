package com.hub.synhub_backend.metrics.domain.model.valueobjects;

import java.util.Map;

public record TaskDistribution(
        String type,
        int value,
        Map<String, MemberTaskCount> details
) {}
