package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.domain.model.commands.CreateUserLeaderCommand;

public class CreateUserLeaderCommandFromResourceAssembler {
    public static CreateUserLeaderCommand toCommandFromResource(Long userId) {
        return new CreateUserLeaderCommand(userId);
    }
}
