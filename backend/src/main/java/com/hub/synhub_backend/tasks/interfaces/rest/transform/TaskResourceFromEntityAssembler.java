package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.groups.interfaces.rest.transform.GroupResourceReducedFromEntityAssembler;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.TaskResource;

public class TaskResourceFromEntityAssembler {
    public static TaskResource toResourceFromEntity(Task entity){
        return new TaskResource(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDueDate().toString(),
                entity.getCreatedAt().toString(),
                entity.getUpdatedAt().toString(),
                entity.getStatus().toString(),
                GroupResourceReducedFromEntityAssembler.toResourceFromEntity(entity.getProfile().getGroup()),
                UserResourceFromEntityAssembler.toResourceFromEntity(entity.getProfile().getUser()),
                entity.getDifficulty(),
                entity.getContext(),
                entity.getUrgency(),
                entity.getLabels(),
                entity.getAiAccepted()
        );
    }
}
