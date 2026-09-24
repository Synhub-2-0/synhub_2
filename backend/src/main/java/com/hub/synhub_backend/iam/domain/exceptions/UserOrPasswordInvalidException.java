package com.hub.synhub_backend.iam.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;

public class UserOrPasswordInvalidException extends ResourceNotFoundException {
    public UserOrPasswordInvalidException(String message) {
        super(message);
    }
}
