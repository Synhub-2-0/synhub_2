package com.hub.synhub_backend.requests.domain.model.valueobjects;

public enum RequestType {
    SUBMISSION,
    MODIFICATION,
    EXPIRED;

    public static RequestType fromString(String type) {
        for (RequestType requestType : RequestType.values()) {
            if (requestType.name().equalsIgnoreCase(type)) {
                return requestType;
            }
        }
        throw new IllegalArgumentException("Unknown request type: " + type);
    }
}