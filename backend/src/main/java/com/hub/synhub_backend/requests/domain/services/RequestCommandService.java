package com.hub.synhub_backend.requests.domain.services;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteAllRequestsByTaskIdCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;

import java.util.Optional;

public interface RequestCommandService {
    Long handle(CreateRequestCommand command);
    Optional<Request> handle(UpdateRequestCommand command);
    void handle(DeleteRequestCommand command);
    void handle(DeleteAllRequestsByTaskIdCommand command);
}
