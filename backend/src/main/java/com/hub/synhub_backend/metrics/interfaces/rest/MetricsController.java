package com.hub.synhub_backend.metrics.interfaces.rest;

import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;
import com.hub.synhub_backend.metrics.interfaces.rest.transform.MetricsResourceFromValueAssembler;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de tareas (implementación base, decisión HUM-05). Solo lectura.
 * <ul>
 *   <li>Métricas del grupo: cualquier integrante.</li>
 *   <li>Métricas de un integrante: el líder o el propio integrante.</li>
 *   <li>Métricas de una tarea: cualquier integrante de su grupo.</li>
 * </ul>
 */
@RestController
@RequestMapping(value = "/api/v1/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Metrics", description = "Provides access to analytics and group metrics")
public class MetricsController {

    private final TaskMetricsQueryService metricsQueryService;
    private final UserQueryService userQueryService;
    private final GroupsContextFacade groupsContextFacade;
    private final TasksContextFacade tasksContextFacade;

    public MetricsController(TaskMetricsQueryService metricsQueryService, UserQueryService userQueryService,
                             GroupsContextFacade groupsContextFacade, TasksContextFacade tasksContextFacade) {
        this.metricsQueryService = metricsQueryService;
        this.userQueryService = userQueryService;
        this.groupsContextFacade = groupsContextFacade;
        this.tasksContextFacade = tasksContextFacade;
    }

    // ---- Control de acceso
    private Long requesterId(UserDetails userDetails) {
        return userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private void requireGroupMember(Long groupId, UserDetails userDetails) {
        if (groupsContextFacade.fetchLeaderIdByGroupId(groupId).isEmpty())
            throw new ResourceNotFoundException("Group with id " + groupId + " does not exist");
        if (!groupsContextFacade.isMember(groupId, requesterId(userDetails)))
            throw new AccessDeniedException("Only members of the group can see its metrics");
    }

    private void requireLeaderOrSelf(Long groupId, Long userId, UserDetails userDetails) {
        requireGroupMember(groupId, userDetails);
        var requester = requesterId(userDetails);
        if (!requester.equals(userId) && !groupsContextFacade.isLeader(groupId, requester))
            throw new AccessDeniedException("Only the leader or the member can see the member's metrics");
        if (!groupsContextFacade.isMember(groupId, userId))
            throw new ResourceNotFoundException("User with id " + userId + " is not in group with id " + groupId);
    }

    private void requireTaskMember(Long taskId, UserDetails userDetails) {
        var task = tasksContextFacade.getTaskById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
        if (!groupsContextFacade.isMember(task.getProfile().getGroup().getId(), requesterId(userDetails)))
            throw new AccessDeniedException("Only members of the group can see the task's metrics");
    }

    // ---- Grupo
    @GetMapping("/groups/{groupId}/overview")
    @Operation(summary = "Tasks of the group by status")
    public ResponseEntity<TaskOverviewResource> groupOverview(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails user) {
        requireGroupMember(groupId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskOverviewInGroupQuery(groupId))));
    }

    @GetMapping("/groups/{groupId}/distribution")
    @Operation(summary = "Tasks of the group per member")
    public ResponseEntity<TaskDistributionResource> groupDistribution(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails user) {
        requireGroupMember(groupId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskDistributionInGroupQuery(groupId))));
    }

    @GetMapping("/groups/{groupId}/rescheduled")
    @Operation(summary = "Times the group's tasks were rescheduled")
    public ResponseEntity<RescheduledTasksResource> groupRescheduled(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails user) {
        requireGroupMember(groupId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetRescheduledTasksInGroupQuery(groupId))));
    }

    @GetMapping("/groups/{groupId}/avg-completion-time")
    @Operation(summary = "Average days to finish (DONE) a task in the group")
    public ResponseEntity<AvgCompletionTimeResource> groupAvgCompletionTime(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails user) {
        requireGroupMember(groupId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetAvgCompletionTimeInGroupQuery(groupId))));
    }

    // ---- Integrante dentro del grupo
    @GetMapping("/groups/{groupId}/members/{userId}/overview")
    @Operation(summary = "Tasks of a member in the group by status")
    public ResponseEntity<TaskOverviewResource> memberOverview(@PathVariable Long groupId, @PathVariable Long userId, @AuthenticationPrincipal UserDetails user) {
        requireLeaderOrSelf(groupId, userId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskOverviewForUserInGroupQuery(userId, groupId))));
    }

    @GetMapping("/groups/{groupId}/members/{userId}/distribution")
    @Operation(summary = "Number of tasks of a member in the group")
    public ResponseEntity<TaskDistributionResource> memberDistribution(@PathVariable Long groupId, @PathVariable Long userId, @AuthenticationPrincipal UserDetails user) {
        requireLeaderOrSelf(groupId, userId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskDistributionForUserInGroupQuery(userId, groupId))));
    }

    @GetMapping("/groups/{groupId}/members/{userId}/rescheduled")
    @Operation(summary = "Times a member's tasks in the group were rescheduled")
    public ResponseEntity<RescheduledTasksResource> memberRescheduled(@PathVariable Long groupId, @PathVariable Long userId, @AuthenticationPrincipal UserDetails user) {
        requireLeaderOrSelf(groupId, userId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetRescheduledTasksForUserInGroupQuery(userId, groupId))));
    }

    @GetMapping("/groups/{groupId}/members/{userId}/avg-completion-time")
    @Operation(summary = "Average days a member takes to finish (DONE) a task in the group")
    public ResponseEntity<AvgCompletionTimeResource> memberAvgCompletionTime(@PathVariable Long groupId, @PathVariable Long userId, @AuthenticationPrincipal UserDetails user) {
        requireLeaderOrSelf(groupId, userId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetAvgCompletionTimeForUserInGroupQuery(userId, groupId))));
    }

    @GetMapping("/groups/{groupId}/members/{userId}/time-passed")
    @Operation(summary = "Average time (ms) a member's tasks in the group have taken")
    public ResponseEntity<TaskTimePassedResource> memberTimePassed(@PathVariable Long groupId, @PathVariable Long userId, @AuthenticationPrincipal UserDetails user) {
        requireLeaderOrSelf(groupId, userId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskTimePassedForUserInGroupQuery(userId, groupId))));
    }

    // ---- Una tarea
    @GetMapping("/tasks/{taskId}/time-passed")
    @Operation(summary = "Time (ms) a COMPLETED or DONE task took", description = "409 if the task is not finished")
    public ResponseEntity<TaskTimePassedResource> taskTimePassed(@PathVariable Long taskId, @AuthenticationPrincipal UserDetails user) {
        requireTaskMember(taskId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskTimePassedByIdQuery(taskId))));
    }

    @GetMapping("/tasks/{taskId}/duration")
    @Operation(summary = "Planned duration (hours) of an IN_PROGRESS task", description = "409 if the task is not IN_PROGRESS")
    public ResponseEntity<TaskDurationResource> taskDuration(@PathVariable Long taskId, @AuthenticationPrincipal UserDetails user) {
        requireTaskMember(taskId, user);
        return ResponseEntity.ok(MetricsResourceFromValueAssembler.toResource(metricsQueryService.handle(new GetTaskDurationByIdQuery(taskId))));
    }
}
