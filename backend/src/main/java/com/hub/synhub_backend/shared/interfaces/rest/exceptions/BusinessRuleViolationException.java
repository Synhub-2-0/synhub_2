package com.hub.synhub_backend.shared.interfaces.rest.exceptions;

public abstract class BusinessRuleViolationException extends RuntimeException {
    protected BusinessRuleViolationException(String message) {
        super(message);
    }
}
