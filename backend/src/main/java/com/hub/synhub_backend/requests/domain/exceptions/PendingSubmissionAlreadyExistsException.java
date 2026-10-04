package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class PendingSubmissionAlreadyExistsException extends BusinessRuleViolationException {
    public PendingSubmissionAlreadyExistsException(Long taskId) {
        super("Task with id " + taskId + " already has a pending submission");
    }
}
