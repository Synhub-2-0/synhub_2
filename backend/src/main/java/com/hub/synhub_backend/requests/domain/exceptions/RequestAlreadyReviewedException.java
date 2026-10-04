package com.hub.synhub_backend.requests.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

public class RequestAlreadyReviewedException extends BusinessRuleViolationException {
    public RequestAlreadyReviewedException(Long requestId, String status) {
        super("Request with id " + requestId + " was already reviewed: " + status);
    }
}
