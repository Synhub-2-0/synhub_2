package com.hub.synhub_backend.iam.interfaces.rest.transform;

import com.hub.synhub_backend.iam.interfaces.rest.resources.UserMemberResource;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

public class UserMemberResourceFromEntityAssembler {
    public static UserMemberResource toResourceFromEntity(Member member) {
        return new UserMemberResource(
                member.getGroup() != null ? member.getGroup().getId() : 0
        );
    }
}
