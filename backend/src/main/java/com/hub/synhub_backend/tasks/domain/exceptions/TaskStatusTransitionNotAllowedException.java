package com.hub.synhub_backend.tasks.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class TaskStatusTransitionNotAllowedException extends BusinessRuleViolationException {
    public TaskStatusTransitionNotAllowedException(String message) {
        super(message);
    }
}
