package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Al bajar de plan hay que elegir exactamente tantos grupos activos como permite el nuevo límite. */
public class InvalidActiveGroupSelectionException extends BusinessRuleViolationException {
    public InvalidActiveGroupSelectionException(int limit) {
        super("The new plan allows " + limit + " active groups: choose exactly " + limit
                + " of the groups you lead that are currently active");
    }
}
