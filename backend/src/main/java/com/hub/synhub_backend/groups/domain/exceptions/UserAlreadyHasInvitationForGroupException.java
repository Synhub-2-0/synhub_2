package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class UserAlreadyHasInvitationForGroupException extends BusinessRuleViolationException {
    public UserAlreadyHasInvitationForGroupException(String message) {
        super(message);
    }
}
