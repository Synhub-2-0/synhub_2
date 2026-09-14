package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByCodeQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByLeaderIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByMemberIdQuery;

import java.util.Optional;

public interface GroupQueryService {
    Optional<Group> handle(GetGroupByLeaderIdQuery query);
    Optional<Group> handle(GetGroupByCodeQuery query);
    Optional<Group> handle(GetGroupByMemberIdQuery query);
    Optional<Group> handle(GetGroupByIdQuery query);
}
