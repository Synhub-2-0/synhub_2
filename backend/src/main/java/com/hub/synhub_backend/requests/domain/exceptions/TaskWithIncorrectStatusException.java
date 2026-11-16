package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class TaskWithIncorrectStatusException extends BusinessRuleViolationException {
    public TaskWithIncorrectStatusException(String message) {
        super(message);
    }
}
