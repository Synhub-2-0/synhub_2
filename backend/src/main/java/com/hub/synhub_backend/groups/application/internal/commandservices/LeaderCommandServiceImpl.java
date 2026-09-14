package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.domain.model.commands.CreateLeaderCommand;
import com.hub.synhub_backend.groups.domain.services.LeaderCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.LeaderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeaderCommandServiceImpl implements LeaderCommandService {

    private final LeaderRepository leaderRepository;

    public LeaderCommandServiceImpl(LeaderRepository leaderRepository) {
        this.leaderRepository = leaderRepository;
    }


    @Override
    public Optional<Leader> handle(CreateLeaderCommand command) {
        Leader leader = new Leader();
        leaderRepository.save(leader);
        return Optional.of(leader);
    }
}


