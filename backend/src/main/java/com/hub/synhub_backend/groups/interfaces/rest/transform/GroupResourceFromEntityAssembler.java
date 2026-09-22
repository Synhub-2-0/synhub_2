package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResource;

public class GroupResourceFromEntityAssembler {
    public static GroupResource toResourceFromEntity(Group group) {
        var userProfiles = group.getUserProfiles();

        return new GroupResource(
                group.getId(),
                group.getName(),
                group.getImgUrl().imgUrl(),
                group.getDescription(),
                group.getCode().code(),
                userProfiles.stream().map(profile -> UserInGroupResourceFromEntityAssembler.toResourceFromEntity(profile.getUser(), profile.getRoleInGroup())).toList()
        );
    }
}
