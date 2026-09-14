package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.ExtendedGroupResource;

public class ExtendedGroupResourceFromEntityAssembler {
    public static ExtendedGroupResource toResourceFromEntity(Group group) {
        return new ExtendedGroupResource(
                group.getId(),
                group.getName(),
                group.getImgUrl().imgUrl(),
                group.getDescription(),
                group.getCode().code(),
                group.getMembers().stream()
                        .map(member -> MemberResourceFromEntityAssembler.toResourceFromEntity(member))
                        .toList()
        );
    }
}
