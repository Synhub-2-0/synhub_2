package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateInvitationResource;

public class CreateInvitationCommandFromResourceAssembler {
    public static CreateInvitationCommand toCommandFromResource(Long requesterId, CreateInvitationResource resource) {
        return new CreateInvitationCommand(
                requesterId,
                resource.userId(),
                resource.groupId()
        );
    }
}
