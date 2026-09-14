package com.hub.synhub_backend.groups.application.internal.acl;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.LeaderRepository;
import com.hub.synhub_backend.groups.interfaces.rest.acl.LeaderContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeaderContextFacadeImpl implements LeaderContextFacade {

    private final LeaderRepository leaderRepository;

    public LeaderContextFacadeImpl(LeaderRepository leaderRepository) {
        this.leaderRepository = leaderRepository;
    }

    @Override
    public Optional<Leader> createLeader() {
        var leader = new Leader();
        var createdLeader = leaderRepository.save(leader);
        return Optional.of(createdLeader);
    }
}
