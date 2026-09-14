package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.interfaces.rest.resources.LeaderResource;

public class LeaderResourceFromEntityAssembler {
    public static LeaderResource toResourceFromEntity(Leader leader) {
        return new LeaderResource(
                leader.getUser().getUsername(),
                leader.getUser().getName(),
                leader.getUser().getSurname(),
                leader.getUser().getImgUrl(),
                leader.getUser().getEmail(),
                leader.getAverageSolutionTime().toString(),
                leader.getSolvedRequests()
        );
    }
}
