package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Una solicitud de modificación pendiente no se revisa ni se borra: se resuelve cuando el líder edita la tarea. */
public class ModificationResolvedByEditingException extends BusinessRuleViolationException {
    public ModificationResolvedByEditingException(Long requestId) {
        super("Modification request " + requestId
                + " is resolved by editing the task: it cannot be reviewed or deleted");
    }
}
