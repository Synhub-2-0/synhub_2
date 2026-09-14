package com.hub.synhub_backend.iam.interfaces.rest.resources;

public record UserLeaderResource(
        String averageSolutionTime,
        Integer solvedRequests
) {
}
