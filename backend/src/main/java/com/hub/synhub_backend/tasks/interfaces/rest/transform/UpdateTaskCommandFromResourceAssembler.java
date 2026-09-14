package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.UpdateTaskResource;

public class UpdateTaskCommandFromResourceAssembler {
    public static UpdateTaskCommand toCommandFromResource(UpdateTaskResource command, Long taskId) {
        return new UpdateTaskCommand(
                taskId,
                command.title(),
                command.description(),
                command.dueDate(),
                command.memberId()
        );

    }
}
