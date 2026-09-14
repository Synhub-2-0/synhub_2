package com.hub.synhub_backend.metrics.infrastructure.client;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.metrics.domain.model.aggregates.GroupMetrics;
import com.hub.synhub_backend.metrics.infrastructure.persistenence.jpa.repositories.GroupMetricsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GroupsClient implements GroupMetricsRepository {

    private final GroupQueryService groupQueryService;

    public GroupsClient(GroupQueryService groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    @Override
    public Optional<GroupMetrics> getGroupMetrics(Long groupId) {
        return groupQueryService.handle(new GetGroupByIdQuery(groupId))
                .map(group -> new GroupMetrics(groupId, group.getMemberCount()));
    }
}
