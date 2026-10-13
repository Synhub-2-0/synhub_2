package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class TaskNotCompletedException extends BusinessRuleViolationException {
    public TaskNotCompletedException(String message) {
        super(message);
    }
}
