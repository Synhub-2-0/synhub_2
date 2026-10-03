package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.commands.UpdateGroupCommand;
import com.hub.synhub_backend.groups.interfaces.rest.resources.UpdateGroupResource;

public class UpdateGroupCommandFromResourceAssembler {
    public static UpdateGroupCommand toCommandFromResource(UpdateGroupResource resource, Long groupId, Long requesterId) {
        return new UpdateGroupCommand(
                requesterId,
                groupId,
                resource.name(),
                resource.description(),
                resource.imgUrl()
        );
    }
}
