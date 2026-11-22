package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/**
 * Una solicitud de modificación pendiente no se aprueba ni se borra: se aprueba cuando el líder edita la tarea. Lo
 * único que se hace desde la bandeja es denegarla (REJECTED), que reanuda la tarea.
 */
public class ModificationResolvedByEditingException extends BusinessRuleViolationException {
    public ModificationResolvedByEditingException(Long requestId) {
        super("Modification request " + requestId
                + " is resolved by editing the task: it cannot be approved or deleted, only denied");
    }
}
