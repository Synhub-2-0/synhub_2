package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.commands.CreateGroupCommand;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateGroupResource;

public class CreateGroupCommandFromResourceAssembler {
    public static CreateGroupCommand toCommandFromResource(CreateGroupResource resource, Long leaderId) {
        return new CreateGroupCommand(
                resource.name(),
                resource.imgUrl(),
                resource.description(),
                leaderId
        );
    }
}
