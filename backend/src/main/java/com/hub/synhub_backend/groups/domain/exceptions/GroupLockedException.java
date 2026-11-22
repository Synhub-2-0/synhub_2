package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Un grupo bloqueado (su líder superó el límite de su plan) es de solo lectura hasta que se activa. */
public class GroupLockedException extends BusinessRuleViolationException {
    public GroupLockedException(Long groupId) {
        super(
                "Group with id " + groupId
                        + " is locked because its leader exceeded the group limit of the plan: it is read-only until it is activated");
    }
}
