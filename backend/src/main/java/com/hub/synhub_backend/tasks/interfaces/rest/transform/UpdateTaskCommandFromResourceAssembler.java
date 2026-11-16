package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskClassificationUpdate;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.UpdateTaskResource;

public class UpdateTaskCommandFromResourceAssembler {
    public static UpdateTaskCommand toCommandFromResource(UpdateTaskResource resource, Long taskId, Long requesterId) {
        var classification = resource.classification();
        return new UpdateTaskCommand(
                requesterId,
                taskId,
                resource.title(),
                resource.description(),
                resource.dueDate(),
                resource.userId(),
                classification == null
                        ? null
                        : new TaskClassificationUpdate(
                                classification.difficulty(),
                                classification.context(),
                                classification.urgency(),
                                classification.labels(),
                                classification.aiAccepted()));
    }
}
