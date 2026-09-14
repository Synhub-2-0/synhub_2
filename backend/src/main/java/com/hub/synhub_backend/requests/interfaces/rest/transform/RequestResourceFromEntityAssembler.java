package com.hub.synhub_backend.requests.interfaces.rest.transform;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.interfaces.rest.resources.RequestResource;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.TaskResourceFromEntityAssembler;

public class RequestResourceFromEntityAssembler {
    public static RequestResource toResourceFromEntity(Request entity) {
        return new RequestResource(
                entity.getId(),
                entity.getDescription(),
                entity.getRequestType(),
                entity.getRequestStatus(),
                TaskResourceFromEntityAssembler.toResourceFromEntity(entity.getTask())
        );
    }
}
