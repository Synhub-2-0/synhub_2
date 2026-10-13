package com.hub.synhub_backend.tasks.domain.exceptions;

import com.hub.synhub_backend.shared.interfaces.rest.exceptions.BusinessRuleViolationException;

/** Una tarea DONE (entrega aprobada) es definitiva (409, AUDIT BUG-18). */
public class TaskFinishedException extends BusinessRuleViolationException {
    public TaskFinishedException(Long taskId) {
        super("Task with id " + taskId + " is DONE and can no longer be edited, moved or deleted");
    }
}
