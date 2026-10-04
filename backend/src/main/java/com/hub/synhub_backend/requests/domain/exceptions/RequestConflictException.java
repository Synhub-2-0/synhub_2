package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class RequestConflictException extends BusinessRuleViolationException {
    public RequestConflictException(String message) {
        super(message);
    }
}
