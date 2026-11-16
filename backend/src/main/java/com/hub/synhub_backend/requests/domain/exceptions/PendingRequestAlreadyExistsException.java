package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class PendingRequestAlreadyExistsException extends BusinessRuleViolationException {
    public PendingRequestAlreadyExistsException(Long taskId) {
        super("Task with id " + taskId + " already has a pending request. Wait for it to be resolved before creating a new one");
    }
}
