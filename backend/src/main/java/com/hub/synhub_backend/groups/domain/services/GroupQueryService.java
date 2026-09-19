package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByCodeQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByLeaderIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByUserIdQuery;

import java.util.List;
import java.util.Optional;

public interface GroupQueryService {
    List<Group> handle(GetGroupsByLeaderIdQuery query);
    Optional<Group> handle(GetGroupByCodeQuery query);
    List<Group> handle(GetGroupsByUserIdQuery query);
    Optional<Group> handle(GetGroupByIdQuery query);
}
