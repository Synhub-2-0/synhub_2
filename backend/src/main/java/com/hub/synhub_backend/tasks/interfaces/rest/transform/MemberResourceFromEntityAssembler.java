package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.MemberResource;

public class MemberResourceFromEntityAssembler {
    public static MemberResource toResourceFromEntity(Member entity) {
        return new MemberResource(
                entity.getId(),
                entity.getUser().getUsername(),
                entity.getUser().getName(),
                entity.getUser().getSurname(),
                entity.getUser().getImgUrl(),
                entity.getUser().getEmail()
        );
    }
}
