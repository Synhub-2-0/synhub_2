package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;

import java.util.Optional;

public interface GroupCommandService {
    Optional<Group> handle(CreateGroupCommand command);
    Optional<Group> handle(UpdateGroupCommand command);
    void handle(DeleteGroupCommand command);
    void handle(RemoveMemberFromGroupCommand command);
    void handle(LeaveGroupCommand command);
}
