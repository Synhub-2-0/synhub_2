package com.hub.synhub_backend.requests.application.internal.commandservices;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteAllRequestsByTaskIdCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RequestCommandServiceImpl implements RequestCommandService {

    private final RequestRepository requestRepository;
    private final TasksContextFacade tasksContextFacade;

    public RequestCommandServiceImpl(
            RequestRepository requestRepository,
            TasksContextFacade tasksContextFacade) {
        this.requestRepository = requestRepository;
        this.tasksContextFacade = tasksContextFacade;
    }

    @Override
    public Long handle(CreateRequestCommand command) {
        try {
            RequestType.fromString(command.requestType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type");
        }

        var task = this.tasksContextFacade.getTaskById(command.taskId());

        if (task.isEmpty())
            throw new IllegalArgumentException("Task with id " + command.taskId() + " does not exist");

        var request = new Request(command);
        request.setTask(task.get());
        this.requestRepository.save(request);
        return request.getId();
    }

    @Override
    public Optional<Request> handle(UpdateRequestCommand command) {
        var requestId = command.requestId();

        if (!this.requestRepository.existsById(requestId))
            throw new IllegalArgumentException("Request with id " + requestId + " does not exist");

        var requestToUpdate = this.requestRepository.findById(requestId).get();
        requestToUpdate.updateRequestStatus(command.requestStatus());

        try {
            var updatedRequest = this.requestRepository.save(requestToUpdate);
            return Optional.of(updatedRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while updating request: " + e.getMessage());
        }
    }

    @Override
    public void handle(DeleteRequestCommand command) {
        var requestId = command.requestId();

        if (!requestRepository.existsById(requestId))
            throw new IllegalArgumentException("Request with id " + requestId + " does not exist");

        try {
            requestRepository.deleteById(requestId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while deleting request: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handle(DeleteAllRequestsByTaskIdCommand command) {
        var taskId = command.taskId();

        if (this.tasksContextFacade.getTaskById(taskId).isEmpty())
            throw new IllegalArgumentException("Task with id " + taskId + " does not exist");

        try {
            this.requestRepository.deleteByTaskId(taskId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while deleting requests for task: " + e.getMessage());
        }
    }
}
