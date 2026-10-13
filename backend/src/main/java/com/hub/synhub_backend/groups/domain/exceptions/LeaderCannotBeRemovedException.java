package com.hub.synhub_backend.groups.domain.exceptions;

public class LeaderCannotBeRemovedException extends RuntimeException {
    public LeaderCannotBeRemovedException(String message) {
        super(message);
    }
}
