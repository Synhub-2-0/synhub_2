package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class ChangesDuringHoldException extends BusinessRuleViolationException {
    public ChangesDuringHoldException(String message) {
        super(message);
    }
}
