package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class NoFreeGroupSlotException extends BusinessRuleViolationException {
    public NoFreeGroupSlotException(int limit) {
        super("Your plan allows " + limit
                + " active groups and all of them are in use: delete one of your active groups or change your plan");
    }
}
