package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

public class InvitationResourceFromEntityAssembler {
    public static InvitationResource toResourceFromEntity(
            Invitation invitation,
            Member member
    ) {
        return new InvitationResource(
                invitation.getId(),
                InvitationMemberResourceFromEntityAssembler.toResourceFromEntity(member),
                GroupResourceFromEntityAssembler.toResourceFromEntity(invitation.getGroup())
        );
    }
}
