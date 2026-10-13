package com.hub.synhub_backend.groups.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;

public class UserNotInGroupException extends ResourceNotFoundException {
    public UserNotInGroupException(String message) {
        super(message);
    }
}
