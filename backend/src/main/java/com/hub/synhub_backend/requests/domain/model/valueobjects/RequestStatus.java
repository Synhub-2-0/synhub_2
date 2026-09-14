package com.hub.synhub_backend.requests.domain.model.valueobjects;

public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static RequestStatus fromString(String status) {
        for (RequestStatus requestStatus : RequestStatus.values()) {
            if (requestStatus.name().equalsIgnoreCase(status)) {
                return requestStatus;
            }
        }
        throw new IllegalArgumentException("Unknown request status: " + status);
    }
}
