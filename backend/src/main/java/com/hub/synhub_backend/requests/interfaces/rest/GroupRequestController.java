package com.hub.synhub_backend.requests.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByLeaderIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByUsernameQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.domain.services.LeaderQueryService;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.interfaces.rest.resources.RequestResource;
import com.hub.synhub_backend.requests.interfaces.rest.transform.RequestResourceFromEntityAssembler;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByGroupIdQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByMemberId;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMemberByUsernameQuery;
import com.hub.synhub_backend.tasks.domain.services.MemberQueryService;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.TaskResourceFromEntityAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1")
@Tag(name = "Group Requests", description = "Group Requests management API")
public class GroupRequestController {

    private final RequestQueryService requestQueryService;
    // Both leader, member, group and task should be in an external service
    // For now the queries are directly in the controller
    private final LeaderQueryService leaderQueryService;
    private final MemberQueryService memberQueryService;
    private final GroupQueryService groupQueryService;
    private final TaskQueryService taskQueryService;

    public GroupRequestController(
            RequestQueryService requestQueryService,
            LeaderQueryService leaderQueryService,
            MemberQueryService memberQueryService,
            GroupQueryService groupQueryService,
            TaskQueryService taskQueryService) {
        this.requestQueryService = requestQueryService;
        this.leaderQueryService = leaderQueryService;
        this.memberQueryService = memberQueryService;
        this.groupQueryService = groupQueryService;
        this.taskQueryService = taskQueryService;
    }

    @GetMapping("/leader/group/requests")
    @Operation(summary = "Get all requests from a group", description = "Get all requests from a group")
    public ResponseEntity<List<RequestResource>> getAllRequestsFromGroup(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        var getLeaderByUsernameQuery = new GetLeaderByUsernameQuery(username);
        var leader = this.leaderQueryService.handle(getLeaderByUsernameQuery);
        if (leader.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var getGroupByLeaderIdQuery = new GetGroupByLeaderIdQuery(leader.get().getId());
        var group = this.groupQueryService.handle(getGroupByLeaderIdQuery);
        if (group.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var getAllTasksByGroupIdQuery = new GetAllTasksByGroupIdQuery(group.get().getId());
        var tasks = taskQueryService.handle(getAllTasksByGroupIdQuery);
        var taskResources = tasks.stream()
                .map(TaskResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        // Get all requests from each task
        var requestResources = taskResources.stream()
                .flatMap(task -> {
                    var getRequestsByTaskIdQuery = new GetRequestsByTaskIdQuery(task.id());
                    return requestQueryService.handle(getRequestsByTaskIdQuery)
                            .stream()
                            .map(RequestResourceFromEntityAssembler::toResourceFromEntity);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(requestResources);

    }

    @GetMapping("/member/group/requests")
    @Operation(summary = "Get all requests from member", description = "Get all requests from member")
    public ResponseEntity<List<RequestResource>> getAllRequestsFromMember(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        var getMemberByUsername = new GetMemberByUsernameQuery(username);
        var member = this.memberQueryService.handle(getMemberByUsername);
        if (member.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var getAllTasksByMemberId = new GetAllTasksByMemberId(member.get().getId());
        var tasks = taskQueryService.handle(getAllTasksByMemberId);
        var taskResources = tasks.stream()
                .map(TaskResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        var requestResources = taskResources.stream()
                .flatMap(task -> {
                    var getRequestByTaskIdQuery = new GetRequestsByTaskIdQuery(task.id());
                    return requestQueryService.handle(getRequestByTaskIdQuery)
                            .stream()
                            .map(RequestResourceFromEntityAssembler::toResourceFromEntity);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(requestResources);
    }
}