package com.hub.synhub_backend.metrics.domain.model.valueobjects;

import java.util.Map;

public record AvgCompletionTime(String type, double value, Map<String, Integer> details) {}
