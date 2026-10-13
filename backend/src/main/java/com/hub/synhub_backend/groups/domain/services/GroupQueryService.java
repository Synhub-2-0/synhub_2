package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface GroupQueryService {
    List<Group> handle(GetGroupsByLeaderIdQuery query);
    Optional<Group> handle(GetGroupByCodeQuery query);
    List<Group> handle(GetGroupsByUserIdQuery query);
    Optional<Group> handle(GetGroupByIdQuery query);
    List<Group> handle(GetGroupsByUserRoleQuery query);
    List<GroupProfile> handle(GetGroupMembersByGroupIdQuery query);
}
