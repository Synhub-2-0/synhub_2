package com.hub.synhub_backend.groups.application.internal.acl;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GroupsContextFacadeImpl implements GroupsContextFacade {

    private final GroupQueryService groupQueryService;

    public GroupsContextFacadeImpl(GroupQueryService groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    @Override
    public Optional<Long> fetchLeaderIdByGroupId(Long groupId) {
        return groupQueryService.handle(new GetGroupByIdQuery(groupId))
                .flatMap(group -> Optional.ofNullable(group.getLeader()))
                .map(User::getId);
    }
}
