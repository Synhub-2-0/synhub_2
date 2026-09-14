package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupMemberResource;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

public class GroupMemberResourceFromEntityAssembler {
    public static GroupMemberResource toResourceFromEntity(Member entity) {
        return new GroupMemberResource(
                entity.getId(),
                entity.getUser().getUsername(),
                entity.getUser().getName(),
                entity.getUser().getSurname(),
                entity.getUser().getImgUrl()
        );
    }
}
