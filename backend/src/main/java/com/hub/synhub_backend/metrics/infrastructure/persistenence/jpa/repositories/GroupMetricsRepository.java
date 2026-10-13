package com.hub.synhub_backend.metrics.infrastructure.persistenence.jpa.repositories;

import com.hub.synhub_backend.metrics.domain.model.aggregates.GroupMetrics;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupMetricsRepository {
    Optional<GroupMetrics> getGroupMetrics(Long groupId);
}
