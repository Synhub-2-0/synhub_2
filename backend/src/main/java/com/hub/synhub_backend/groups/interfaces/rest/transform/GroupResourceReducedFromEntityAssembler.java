package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResourceReduced;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

public class GroupResourceReducedFromEntityAssembler {
    public static GroupResourceReduced toResourceFromEntity(Group group) {
        return new GroupResourceReduced(
                group.getId(),
                group.getName(),
                group.getImgUrl().imgUrl(),
                group.getDescription(),
                group.getCode().code(),
                group.getMemberCount(),
                UserResourceFromEntityAssembler.toResourceFromEntity(group.getLeader())
        );
    }
}
