package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Regla de negocio (409, AUDIT BUG-10): el líder no puede salir ni ser expulsado de su grupo. */
public class LeaderCannotBeRemovedException extends BusinessRuleViolationException {
    public LeaderCannotBeRemovedException(String message) {
        super(message);
    }
}
