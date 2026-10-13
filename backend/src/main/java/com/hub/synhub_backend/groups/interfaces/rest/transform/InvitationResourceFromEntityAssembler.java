package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.iam.interfaces.rest.transform.PublicUserResourceFromEntityAssembler;

public class InvitationResourceFromEntityAssembler {
    public static InvitationResource toResourceFromEntity(Invitation invitation) {
        return new InvitationResource(
                invitation.getId(),
                GroupResourceReducedFromEntityAssembler.toResourceFromEntity(invitation.getGroup()),
                PublicUserResourceFromEntityAssembler.toResourceFromEntity(invitation.getUser()));
    }
}
