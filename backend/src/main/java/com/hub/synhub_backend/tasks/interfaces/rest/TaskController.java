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

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task by id", description = "Get a task by id")
    public ResponseEntity<TaskResource> getTaskById(@PathVariable Long taskId) {
        var getTaskByIdQuery = new GetTaskByIdQuery(taskId);
        var task = this.taskQueryService.handle(getTaskByIdQuery);

        if (task.isEmpty()) return ResponseEntity.notFound().build();

        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task.get());
        return ResponseEntity.ok(taskResource);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get all tasks by status", description = "Get all tasks by status")
    public ResponseEntity<List<TaskResource>> getAllTasksByStatus(@PathVariable String status) {
        var getAllTasksByStatusQuery = new GetAllTaskByStatusQuery(status);
        var tasks = taskQueryService.handle(getAllTasksByStatusQuery);
        var taskResources = tasks.stream()
                .map(TaskResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(taskResources);
    }

    @PutMapping("/{taskId}/status/{status}")
    @Operation(summary = "Update task status", description = "Update task status")
    public ResponseEntity<TaskResource> updateTaskStatus(@PathVariable Long taskId, @PathVariable String status) {
        var updateTaskStatusCommand = new UpdateTaskStatusCommand(taskId, status);
        var task = this.taskCommandService.handle(updateTaskStatusCommand);
        if (task.isEmpty()) return ResponseEntity.badRequest().build();

        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task.get());
        return ResponseEntity.ok(taskResource);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task", description = "Update task")
    public ResponseEntity<TaskResource> updateTask(@PathVariable Long taskId, @RequestBody UpdateTaskResource resource) {
        var updateTaskCommand = UpdateTaskCommandFromResourceAssembler.toCommandFromResource(resource, taskId);
        var task = taskCommandService.handle(updateTaskCommand);

        if (task.isEmpty()) return ResponseEntity.badRequest().build();

        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(task.get());
        return ResponseEntity.ok(taskResource);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task by id", description = "Delete a task by id")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        var deleteRequestsCommand = new DeleteAllRequestsByTaskIdCommand(taskId);
        this.requestCommandService.handle(deleteRequestsCommand);

        var deleteTaskCommand = new DeleteTaskCommand(taskId);
        this.taskCommandService.handle(deleteTaskCommand);


        return ResponseEntity.noContent().build();
    }

}
