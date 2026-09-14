package com.hub.synhub_backend.metrics.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByLeaderIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByUsernameQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.domain.services.LeaderQueryService;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.model.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Provides access to analytics and group metrics")
public class MetricsController {

    private final TaskMetricsQueryService taskMetricsQueryService;
    private final LeaderQueryService leaderQueryService;
    private final GroupQueryService groupQueryService;

    public MetricsController(TaskMetricsQueryService taskMetricsQueryService, LeaderQueryService leaderQueryService, GroupQueryService groupQueryService) {
        this.taskMetricsQueryService = taskMetricsQueryService;
        this.leaderQueryService = leaderQueryService;
        this.groupQueryService = groupQueryService;
    }

    private Optional<Long> getGroupIdFromUser(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        var leader = leaderQueryService.handle(new GetLeaderByUsernameQuery(username));
        if (leader.isEmpty()) return Optional.empty();
        var group = groupQueryService.handle(new GetGroupByLeaderIdQuery(leader.get().getId()));
        return group.map(AuditableAbstractAggregateRoot::getId);
    }

    @GetMapping("/task/member/{memberId}/time-passed")
    @Operation(
        summary = "Get time passed for a member's completed task",
        description = "Returns the time passed in milliseconds for a completed task assigned to the given member.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskTimePassedResource> getAverageTaskTimePassed(@PathVariable Long memberId) {
        var query = new GetTaskTimePassedQuery(memberId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/tasks/overview")
    @Operation(
        summary = "Get task overview for group",
        description = "Returns a summary of task statuses for the authenticated leader's group.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskOverviewResource> getTaskOverview(@AuthenticationPrincipal UserDetails userDetails) {
        var groupIdOpt = getGroupIdFromUser(userDetails);
        if (groupIdOpt.isEmpty()) return ResponseEntity.notFound().build();
        var query = new GetTaskOverviewQuery(groupIdOpt.get());
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/tasks/distribution")
    @Operation(
        summary = "Get task distribution for group",
        description = "Returns the number of tasks assigned to each member in the authenticated leader's group.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskDistributionResource> getTaskDistribution(@AuthenticationPrincipal UserDetails userDetails) {
        var groupIdOpt = getGroupIdFromUser(userDetails);
        if (groupIdOpt.isEmpty()) return ResponseEntity.notFound().build();
        var query = new GetTaskDistributionQuery(groupIdOpt.get());
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/tasks/rescheduled")
    @Operation(
        summary = "Get rescheduled tasks for group",
        description = "Returns the count of rescheduled vs non-rescheduled tasks for the authenticated leader's group, and the memberIds of those with rescheduled tasks.",
        tags = {"Metrics"}
    )
    public ResponseEntity<RescheduledTasksResource> getRescheduledTasks(@AuthenticationPrincipal UserDetails userDetails) {
        var groupIdOpt = getGroupIdFromUser(userDetails);
        if (groupIdOpt.isEmpty()) return ResponseEntity.notFound().build();
        var query = new GetRescheduledTasksQuery(groupIdOpt.get());
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/tasks/avg-completion-time")
    @Operation(
        summary = "Get average completion time for group",
        description = "Returns the average time (in days) it takes to complete tasks in the authenticated leader's group.",
        tags = {"Metrics"}
    )
    public ResponseEntity<AvgCompletionTimeResource> getAvgCompletionTime(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        var leader = leaderQueryService.handle(new GetLeaderByUsernameQuery(username));
        if (leader.isEmpty()) return ResponseEntity.notFound().build();
        var query = new GetAvgCompletionTimeQuery(leader.get().getId());
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/member/{memberId}/tasks/overview")
    @Operation(
        summary = "Get task overview for member",
        description = "Returns a summary of task statuses for the given member.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskOverviewResource> getTaskOverviewForMember(@PathVariable Long memberId) {
        var query = new GetTaskOverviewForMemberQuery(memberId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/member/{memberId}/tasks/distribution")
    @Operation(
        summary = "Get task distribution for member",
        description = "Returns the number of tasks assigned to the given member.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskDistributionResource> getTaskDistributionForMember(@PathVariable Long memberId) {
        var query = new GetTaskDistributionForMemberQuery(memberId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/member/{memberId}/tasks/rescheduled")
    @Operation(
        summary = "Get rescheduled tasks for member",
        description = "Returns the count of rescheduled vs non-rescheduled tasks for the given member.",
        tags = {"Metrics"}
    )
    public ResponseEntity<RescheduledTasksResource> getRescheduledTasksForMember(@PathVariable Long memberId) {
        var query = new GetRescheduledTasksForMemberQuery(memberId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/member/{memberId}/tasks/avg-completion-time")
    @Operation(
        summary = "Get average completion time for member",
        description = "Returns the average time (in days) it takes for the given member to complete tasks.",
        tags = {"Metrics"}
    )
    public ResponseEntity<AvgCompletionTimeResource> getAvgCompletionTimeForMember(@PathVariable Long memberId) {
        var query = new GetAvgCompletionTimeForMemberQuery(memberId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/task/{taskId}/duration")
    @Operation(
        summary = "Get duration of an IN_PROGRESS task",
        description = "Returns the duration in hours for a task that is currently IN_PROGRESS.",
        tags = {"Metrics"}
    )
    public ResponseEntity<TaskDurationResource> getTaskDuration(@PathVariable Long taskId) {
        var query = new GetTaskDurationByIdQuery(taskId);
        var resource = taskMetricsQueryService.handle(query);
        return ResponseEntity.ok(resource);
    }
}