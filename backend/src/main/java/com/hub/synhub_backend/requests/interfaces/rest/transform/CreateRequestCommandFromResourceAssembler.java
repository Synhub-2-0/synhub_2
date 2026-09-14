package com.hub.synhub_backend.requests.interfaces.rest.transform;

import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.interfaces.rest.resources.CreateRequestResource;

public class CreateRequestCommandFromResourceAssembler {
    public static CreateRequestCommand toCommandFromResource(CreateRequestResource resource, Long taskId) {
        return new CreateRequestCommand(
                resource.description(),
                resource.requestType(),
                taskId
        );
    }
}