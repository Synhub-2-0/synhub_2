package com.hub.synhub_backend.groups.interfaces.rest.acl;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;

import java.util.Optional;

public interface LeaderContextFacade {
    Optional<Leader> createLeader();
}
