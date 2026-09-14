package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationMemberResource;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

public class InvitationMemberResourceFromEntityAssembler {
    public static InvitationMemberResource toResourceFromEntity(Member entity) {
        return new InvitationMemberResource(
                entity.getId(),
                entity.getUser().getUsername(),
                entity.getUser().getName(),
                entity.getUser().getSurname(),
                entity.getUser().getImgUrl()
        );
    }
}
