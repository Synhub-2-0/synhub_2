package com.hub.synhub_backend.tasks.interfaces.rest.transform;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.TaskMemberResource;

public class TaskMemberResourceFromEntityAssembler {

    public static TaskMemberResource toResourceFromEntity(Member member) {
        return new TaskMemberResource(
                member.getId(),
                member.getUser().getName(),
                member.getUser().getSurname(),
                member.getUser().getImgUrl()
        );
    }
}
