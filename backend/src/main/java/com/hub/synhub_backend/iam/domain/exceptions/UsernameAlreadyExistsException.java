package com.hub.synhub_backend.iam.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class UsernameAlreadyExistsException extends BusinessRuleViolationException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
