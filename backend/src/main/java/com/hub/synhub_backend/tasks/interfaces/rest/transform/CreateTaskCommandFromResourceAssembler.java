package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.CreateTaskResource;

public class CreateTaskCommandFromResourceAssembler {
    public static CreateTaskCommand toCommandFromResource(CreateTaskResource resource, Long userId) {
        return new CreateTaskCommand(
                resource.title(),
                resource.description(),
                resource.dueDate(),
                resource.userId(),
                resource.groupId()
        );
    }
}
