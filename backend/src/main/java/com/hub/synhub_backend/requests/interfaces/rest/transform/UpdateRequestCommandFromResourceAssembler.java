package com.hub.synhub_backend.requests.interfaces.rest.transform;

import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;

public class UpdateRequestCommandFromResourceAssembler {
    public static UpdateRequestCommand toCommandFromResource(Long taskId, Long requestId, String status, Long requesterId) {
        return new UpdateRequestCommand(taskId, requestId, status, requesterId);
    }
}
