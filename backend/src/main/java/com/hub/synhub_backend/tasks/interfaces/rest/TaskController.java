package com.hub.synhub_backend.tasks.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.queries.GetTasksByGroupIdAndStatus;
import com.hub.synhub_backend.tasks.domain.model.queries.GetTasksByGroupIdAndUserIdQuery;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
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
            UserQueryService userQueryService) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.userQueryService = userQueryService;
    }

    @PostMapping()
    @Operation(summary = "Create a new task", description = "Creates a new task within a group and assigns it to a member. Only the group leader can perform this action.")
    public ResponseEntity<TaskResource> createTask(@RequestBody CreateTaskResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var createTaskCommand = CreateTaskCommandFromResourceAssembler.toCommandFromResource(resource, user.getId());
        var task = this.taskCommandService.handle(createTaskCommand);
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task by ID", description = "Retrieves the details of a single task using its unique identifier.")
    public ResponseEntity<TaskResource> getTaskById(@PathVariable Long taskId) {
        var getTaskByIdQuery = new GetTaskByIdQuery(taskId);
        var task = this.taskQueryService.handle(getTaskByIdQuery)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get tasks by group", description = "Retrieves all tasks belonging to a group, optionally filtered by task status.")
    public ResponseEntity<List<TaskResource>> getByGroup(@PathVariable Long groupId, @RequestParam(required = false) TaskStatus status) {
        var getTasksByGroupIdAndStatusQuery = new GetTasksByGroupIdAndStatus(groupId, status);
        var tasks = this.taskQueryService.handle(getTasksByGroupIdAndStatusQuery);
        var taskResources = tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(taskResources);
    }

    @GetMapping("/group/{groupId}/user/{userId}")
    @Operation(summary = "Get tasks by group and user", description = "Retrieves all tasks within a group that are assigned to a specific user.")
    public ResponseEntity<List<TaskResource>> getTaskByGroupIdAndUserId(@PathVariable Long groupId, @PathVariable Long userId) {
        var getTasksByGroupIdAndUserIdQuery = new GetTasksByGroupIdAndUserIdQuery(groupId, userId);
        var tasks = this.taskQueryService.handle(getTasksByGroupIdAndUserIdQuery);
        var taskResources = tasks.stream().map(TaskResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(taskResources);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task", description = "Deletes an existing task by its ID. Requires the authenticated user to have permission to delete the task.")
    public ResponseEntity<Void> deleteTaskById(@PathVariable Long taskId, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var deleteTaskByIdCommand = new DeleteTaskCommand(user.getId(), taskId);
        this.taskCommandService.handle(deleteTaskByIdCommand);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task details", description = "Updates the editable details of an existing task, such as its title, description, or due date.")
    public ResponseEntity<TaskResource> updateTaskDetails(@PathVariable Long taskId, @RequestBody UpdateTaskResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var updateTaskDetailsCommand = UpdateTaskCommandFromResourceAssembler.toCommandFromResource(resource, taskId, user.getId());
        var task = this.taskCommandService.handle(updateTaskDetailsCommand);
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

    @PutMapping("/{taskId}/status")
    @Operation(summary = "Update task status", description = "Updates the status of an existing task, such as marking it as in progress or completed.")
    public ResponseEntity<TaskResource> updateTaskStatus(@PathVariable Long taskId, @RequestParam TaskStatus status, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var updateTaskStatusCommand = new UpdateTaskStatusCommand(user.getId(), taskId, status);
        var task = this.taskCommandService.handle(updateTaskStatusCommand);
        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task);
        return ResponseEntity.ok(taskResource);
    }

}
