package com.hub.synhub_backend.iam.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class EmailAlreadyExistsException extends BusinessRuleViolationException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
