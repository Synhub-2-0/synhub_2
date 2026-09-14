package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserLeaderResource;

public class UserLeaderResourceFromEntityAssembler {
    public static UserLeaderResource toResourceFromEntity(Leader leader){
        return new UserLeaderResource(
                leader.getAverageSolutionTime().toString(),
                leader.getSolvedRequests()
        );
    }
}
