package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.commands.CreateUserMemberCommand;

public class CreateUserMemberCommandFromResourceAssembler {
    public static CreateUserMemberCommand toCommandFromResource(Long userId) {
        return new CreateUserMemberCommand(userId);
    }
}
