package com.hub.synhub_backend.tasks.domain.model.valueobjects;

public enum TaskStatus {
    ON_HOLD,
    IN_PROGRESS,
    COMPLETED,
    DONE,
    EXPIRED,;

    public static TaskStatus fromString(String status) {
        for (TaskStatus taskStatus : TaskStatus.values()) {
            if (taskStatus.name().equalsIgnoreCase(status)) {
                return taskStatus;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + status);
    }
}
