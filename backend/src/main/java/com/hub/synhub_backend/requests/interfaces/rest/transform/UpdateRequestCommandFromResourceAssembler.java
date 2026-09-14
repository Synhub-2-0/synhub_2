package com.hub.synhub_backend.requests.interfaces.rest.transform;

import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;

public class UpdateRequestCommandFromResourceAssembler {
    public static UpdateRequestCommand toCommandFromResource(Long requestId, String status) {
        return new UpdateRequestCommand(requestId, status);
    }
}