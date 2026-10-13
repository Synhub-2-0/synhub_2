package com.hub.synhub_backend.metrics.domain.model.valueobjects;

import java.util.List;
import java.util.Map;

public record RescheduledTasks(
        String type,
        long value,
        Map<String, Integer> details,
        List<Long> rescheduledMemberIds
) {}
