package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;

public interface InvitationCommandService {
    Invitation handle(CreateInvitationCommand command);
    void handle(CancelInvitationCommand command);
    void handle(AcceptInvitationCommand command);
}
