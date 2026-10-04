package com.hub.synhub_backend.requests.application.internal.commandservices;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteAllRequestsByTaskIdCommand;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.commands.UpdateRequestCommand;
import com.hub.synhub_backend.requests.domain.exceptions.RequestConflictException;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.security.access.AccessDeniedException;
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
        RequestType requestType;
        try {
            requestType = RequestType.fromString(command.requestType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type");
        }

        var task = this.tasksContextFacade.getTaskById(command.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + command.taskId() + " does not exist"));

        // requesterId null = solicitud generada por el sistema (p. ej. vencimiento): sin reglas de usuario.
        if (command.requesterId() != null) {
            if (!task.getProfile().getUser().getId().equals(command.requesterId()))
                throw new AccessDeniedException("Only the assigned member can create requests for this task");
            if (requestType == RequestType.SUBMISSION) {
                if (task.getStatus() != TaskStatus.COMPLETED)
                    throw new RequestConflictException("The task must be completed before submitting it");
                if (this.requestRepository.existsByTask_IdAndRequestTypeAndRequestStatus(task.getId(), RequestType.SUBMISSION, RequestStatus.PENDING))
                    throw new RequestConflictException("The task already has a pending submission");
            }
        }

        var request = new Request(command);
        request.setTask(task);
        this.requestRepository.save(request);
        return request.getId();
    }

    @Override
    @Transactional
    public Optional<Request> handle(UpdateRequestCommand command) {
        var requestToUpdate = this.requestRepository.findById(command.requestId())
                .orElseThrow(() -> new ResourceNotFoundException("Request with id " + command.requestId() + " does not exist"));
        var task = requestToUpdate.getTask();

        if (!task.getProfile().getGroup().getLeader().getId().equals(command.requesterId()))
            throw new AccessDeniedException("Only the group leader can review requests");

        var newStatus = RequestStatus.fromString(command.requestStatus());
        if (newStatus == RequestStatus.PENDING)
            throw new IllegalArgumentException("A request can only be approved or rejected");
        if (!RequestStatus.PENDING.name().equals(requestToUpdate.getRequestStatus()))
            throw new RequestConflictException("The request was already reviewed");

        // Revisar una entrega mueve la tarea: aprobada -> DONE, rechazada -> vuelve a IN_PROGRESS.
        if (RequestType.SUBMISSION.name().equals(requestToUpdate.getRequestType())) {
            if (newStatus == RequestStatus.APPROVED) {
                if (task.getStatus() != TaskStatus.COMPLETED)
                    throw new RequestConflictException("The task is no longer completed");
                this.tasksContextFacade.updateTaskStatus(command.requesterId(), task.getId(), TaskStatus.DONE);
            } else {
                this.tasksContextFacade.updateTaskStatus(command.requesterId(), task.getId(), TaskStatus.IN_PROGRESS);
            }
        }

        requestToUpdate.updateRequestStatus(newStatus.name());
        return Optional.of(this.requestRepository.save(requestToUpdate));
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
