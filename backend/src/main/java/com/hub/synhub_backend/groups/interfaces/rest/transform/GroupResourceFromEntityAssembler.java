package com.hub.synhub_backend.groups.interfaces.rest.transform;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResource;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import java.util.ArrayList;
import java.util.List;

public class GroupResourceFromEntityAssembler {
    public static GroupResource toResourceFromEntity(Group group) {
        List<User> usersInGroup = new ArrayList<>();
        var userProfiles = group.getUserProfiles();

        System.out.println("Users Profiles In Group: " + userProfiles);
        
        userProfiles.forEach(groupProfile -> {
            usersInGroup.add(groupProfile.getUser());
        });

        System.out.println("Users In Group: " + usersInGroup);

        return new GroupResource(
                group.getId(),
                group.getName(),
                group.getImgUrl().imgUrl(),
                group.getDescription(),
                group.getCode().code(),
                group.getMemberCount(),
                usersInGroup.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList()
        );
    }
}
