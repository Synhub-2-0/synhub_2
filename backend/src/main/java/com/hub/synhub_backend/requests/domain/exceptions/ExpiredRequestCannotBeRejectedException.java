package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Una solicitud de vencimiento no se rechaza: el líder reanuda la tarea (con una nueva fecha) y eso la cierra. */
public class ExpiredRequestCannotBeRejectedException extends BusinessRuleViolationException {
    public ExpiredRequestCannotBeRejectedException(Long requestId) {
        super("Expiration request " + requestId + " cannot be rejected: resume the task with a new due date instead");
    }
}
