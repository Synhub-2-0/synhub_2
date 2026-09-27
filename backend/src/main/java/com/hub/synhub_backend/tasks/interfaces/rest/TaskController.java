package com.hub.synhub_backend.tasks.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.CreateTaskResource;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.CreateTaskCommandFromResourceAssembler;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserQueryService userQueryService;

    public TaskController(
            TaskQueryService taskQueryService,
            TaskCommandService taskCommandService,
            RequestCommandService requestCommandService1,
            UserQueryService userQueryService) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping()
    public ResponseEntity<TaskResource> createTask(@RequestBody CreateTaskResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var createTaskCommand = CreateTaskCommandFromResourceAssembler.toCommandFromResource(resource, user.getId());
        var task = this.taskCommandService.handle(createTaskCommand);
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResource> getTaskById(@PathVariable Long taskId) {
        var getTaskByIdQuery = new GetTaskByIdQuery(taskId);
        var task = this.taskQueryService.handle(getTaskByIdQuery)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

    // TODO: Get task by group and status

    // TODO: Update task status

    // TODO: Update task details

    // TODO: Delete a task

    // TODO: Get all Tasks from a Group Id

}
