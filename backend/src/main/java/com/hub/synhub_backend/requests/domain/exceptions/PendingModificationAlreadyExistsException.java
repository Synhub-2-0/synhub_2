package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class PendingModificationAlreadyExistsException extends BusinessRuleViolationException {
    public PendingModificationAlreadyExistsException(Long taskId) {
        super("Task with id " + taskId + " already has a pending modification");
    }
}
