package com.hub.synhub_backend.tasks.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteAllRequestsByTaskIdCommand;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTaskByStatusQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetTaskByIdQuery;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.TaskResource;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.UpdateTaskResource;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.TaskResourceFromEntityAssembler;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.UpdateTaskCommandFromResourceAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/tasks")
@Tag(name = "Task", description = "Task management API")
@ApiResponse(responseCode = "201", description = "Task Created")
public class TaskController {

    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskCommandService;
    private final RequestCommandService requestCommandService;

    public TaskController(TaskQueryService taskQueryService, TaskCommandService taskCommandService, RequestCommandService requestCommandService, RequestCommandService requestCommandService1) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.requestCommandService = requestCommandService1;
    }

    // TODO: Create a task for group and user

    // TODO: Get task by id

    // TODO: Get task by group and status

    // TODO: Update task status

    // TODO: Update task details

    // TODO: Delete a task

    // TODO: Get all Tasks from a Group Id

}
