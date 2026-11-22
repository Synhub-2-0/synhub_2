package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class GroupAlreadyActiveException extends BusinessRuleViolationException {
    public GroupAlreadyActiveException(Long groupId) {
        super("Group with id " + groupId + " is already active");
    }
}
