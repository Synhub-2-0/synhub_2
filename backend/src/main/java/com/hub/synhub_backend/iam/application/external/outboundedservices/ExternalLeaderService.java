package com.hub.synhub_backend.iam.application.external.outboundedservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.interfaces.rest.acl.LeaderContextFacade;
import com.hub.synhub_backend.iam.domain.model.commands.CreateUserLeaderCommand;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExternalLeaderService {
    private final LeaderContextFacade leaderContextFacade;

    public ExternalLeaderService(LeaderContextFacade leaderContextFacade) {
        this.leaderContextFacade = leaderContextFacade;
    }

    public Optional<Leader> createUserLeader(CreateUserLeaderCommand command) {
        var leader = this.leaderContextFacade.createLeader();
        if (leader.isEmpty()) {
            throw new IllegalArgumentException("Error creating leader");
        }
        return leader;
    }
}
