package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class UserAlreadyInGroupException extends BusinessRuleViolationException {
    public UserAlreadyInGroupException(String message) {
        super(message);
    }
}
