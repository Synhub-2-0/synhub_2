package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;

public interface GroupCommandService {
    Group handle(CreateGroupCommand command);
    Group handle(UpdateGroupCommand command);
    void handle(DeleteGroupCommand command);
    void handle(RemoveMemberUserFromGroupCommand command);
}
