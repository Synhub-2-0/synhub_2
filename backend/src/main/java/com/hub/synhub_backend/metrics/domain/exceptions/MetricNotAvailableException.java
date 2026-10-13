package com.hub.synhub_backend.metrics.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** La métrica pedida no aplica al estado actual de la tarea (409). */
public class MetricNotAvailableException extends BusinessRuleViolationException {
    public MetricNotAvailableException(String message) {
        super(message);
    }
}
