package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByUsernameQuery;

import java.util.Optional;

public interface LeaderQueryService {
    Optional<Leader> handle(GetLeaderByIdQuery query);
    Optional<Leader> handle(GetLeaderByUsernameQuery query);
}
