package com.hub.synhub_backend.groups.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByUsernameQuery;
import com.hub.synhub_backend.groups.domain.services.LeaderQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.LeaderRepository;
import com.hub.synhub_backend.shared.application.external.outboundedservices.ExternalIamService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeaderQueryServiceImpl implements LeaderQueryService {

    private final LeaderRepository leaderRepository;
    private final ExternalIamService externalIamService;

    public LeaderQueryServiceImpl(LeaderRepository leaderRepository, ExternalIamService externalIamService) {
        this.leaderRepository = leaderRepository;
        this.externalIamService = externalIamService;
    }

    @Override
    public Optional<Leader> handle(GetLeaderByIdQuery query) {
        return leaderRepository.findById(query.leaderId());
    }

    @Override
    public Optional<Leader> handle(GetLeaderByUsernameQuery query) {
        var user = externalIamService.getUserByUsername(query.username());

        var role = user.get().getRoles().stream().findFirst().get().getName().toString();

        if (!role.equals("ROLE_LEADER")){
            return Optional.empty();
        }

        var leader = user.get().getLeader();
        if (leader == null) {
            return Optional.empty();
        }

        return Optional.of(leader);
    }
}
