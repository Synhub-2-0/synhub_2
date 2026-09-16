package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.UpdateTaskResource;

public class UpdateTaskCommandFromResourceAssembler {
    public static UpdateTaskCommand toCommandFromResource(UpdateTaskResource resource, Long taskId) {
        return new UpdateTaskCommand(
                taskId,
                resource.title(),
                resource.description(),
                resource.dueDate(),
                resource.userId()
        );

    }
}
