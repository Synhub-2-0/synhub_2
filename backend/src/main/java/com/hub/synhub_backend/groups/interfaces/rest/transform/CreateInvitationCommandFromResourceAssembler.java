package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateInvitationResource;

public class CreateInvitationCommandFromResourceAssembler {
    public static CreateInvitationCommand toCommandFromResource(CreateInvitationResource resource) {
        return new CreateInvitationCommand(
                resource.userId(),
                resource.groupId()
        );
    }
}
