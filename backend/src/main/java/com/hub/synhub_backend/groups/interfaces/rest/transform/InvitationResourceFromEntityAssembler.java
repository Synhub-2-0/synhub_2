package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

public class InvitationResourceFromEntityAssembler {
    public static InvitationResource toResourceFromEntity(Invitation invitation) {
        return new InvitationResource(
                invitation.getId(),
                GroupResourceFromEntityAssembler.toResourceFromEntity(invitation.getGroup()),
                UserResourceFromEntityAssembler.toResourceFromEntity(invitation.getUser())
        );
    }
}
