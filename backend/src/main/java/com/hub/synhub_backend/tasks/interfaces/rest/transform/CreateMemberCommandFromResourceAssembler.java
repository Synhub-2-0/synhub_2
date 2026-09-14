package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.commands.CreateMemberCommand;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.CreateMemberResource;

public class CreateMemberCommandFromResourceAssembler {
    public static CreateMemberCommand toCommandFromResource(CreateMemberResource resource) {
        return new CreateMemberCommand(
        );
    }
}
