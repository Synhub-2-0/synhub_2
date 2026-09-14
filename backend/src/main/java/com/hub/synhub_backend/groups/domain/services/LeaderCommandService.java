package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.domain.model.commands.CreateLeaderCommand;

import java.util.Optional;

public interface LeaderCommandService {

    Optional<Leader> handle(CreateLeaderCommand command);
}
