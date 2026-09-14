package com.hub.synhub_backend.tasks.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.commands.RemoveMemberFromGroupCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByMemberIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.tasks.domain.model.commands.DeleteTasksByMemberId;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByMemberId;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMemberByIdQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMemberByUsernameQuery;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.MemberQueryService;
import com.hub.synhub_backend.tasks.domain.services.TaskCommandService;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.ExtendedGroupResource;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.MemberResource;
import com.hub.synhub_backend.tasks.interfaces.rest.resources.TaskResource;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.ExtendedGroupResourceFromEntityAssembler;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.MemberResourceFromEntityAssembler;
import com.hub.synhub_backend.tasks.interfaces.rest.transform.TaskResourceFromEntityAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/member")
@Tag(name = "Member", description = "Member API")
@ApiResponse(responseCode = "201", description = "Member created")
public class MemberController {
    private final MemberQueryService memberQueryService;
    private final GroupQueryService groupQueryService;
    private final GroupCommandService groupCommandService;
    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskCommandService;
    public MemberController(MemberQueryService memberQueryService, GroupQueryService groupQueryService, GroupCommandService groupCommandService, TaskQueryService taskQueryService, TaskCommandService taskCommandService) {
        this.memberQueryService = memberQueryService;
        this.groupQueryService = groupQueryService;
        this.groupCommandService = groupCommandService;
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
    }

    @GetMapping("/details")
    @Operation(summary = "Get member details by authentication", description = "Fetches the details of the authenticated member.")
    public ResponseEntity<MemberResource> getMemberByAuthentication(@AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();

        var getMemberByUsernameQuery = new GetMemberByUsernameQuery(username);

        var member = this.memberQueryService.handle(getMemberByUsernameQuery);

        if(member.isEmpty()) return ResponseEntity.notFound().build();

        var memberResource = MemberResourceFromEntityAssembler.toResourceFromEntity(member.get());

        return ResponseEntity.ok(memberResource);
    }

    @GetMapping("/details/{memberId}")
    @Operation(summary = "Get member details by member ID", description = "Fetches the details of a member by their ID.")
    public ResponseEntity<MemberResource> getMemberById(@PathVariable Long memberId) {
        var getMemberByIdQuery = new GetMemberByIdQuery(memberId);
        var member = this.memberQueryService.handle(getMemberByIdQuery);
        if (member.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var memberResource = MemberResourceFromEntityAssembler.toResourceFromEntity(member.get());
        return ResponseEntity.ok(memberResource);
    }

    @GetMapping("/group")
    @Operation(summary = "Get group by member authenticated", description = "Retrieve the group associated with the authenticated member")
    public ResponseEntity<ExtendedGroupResource> getGroupByMemberId(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        var getMemberByUsernameQuery = new GetMemberByUsernameQuery(username);
        var member = this.memberQueryService.handle(getMemberByUsernameQuery);
        if (member.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var getGroupByMemberIdQuery = new GetGroupByMemberIdQuery(member.get().getId());
        var group = this.groupQueryService.handle(getGroupByMemberIdQuery);
        if (group.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var groupResource = ExtendedGroupResourceFromEntityAssembler.toResourceFromEntity(group.get());
        return ResponseEntity.ok(groupResource);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Get all tasks by authenticated member", description = "Fetches all tasks for the authenticated member.")
    public ResponseEntity<List<TaskResource>> getAllTasksByMemberAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        var getMemberByUsernameQuery = new GetMemberByUsernameQuery(username);

        var member = this.memberQueryService.handle(getMemberByUsernameQuery);

        if(member.isEmpty()) return ResponseEntity.notFound().build();

        var getAllTasksByMemberId = new GetAllTasksByMemberId(member.get().getId());

        var tasks = taskQueryService.handle(getAllTasksByMemberId);

        var taskResources = tasks.stream()
                .map(TaskResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskResources);
    }

    @DeleteMapping("/group/leave")
    @Operation(summary = "Leave group by member authenticated", description = "Allows the authenticated member to leave their group.")
    public ResponseEntity<Void> leaveGroupByMemberAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        var getMemberByUsernameQuery = new GetMemberByUsernameQuery(username);

        var member = this.memberQueryService.handle(getMemberByUsernameQuery);

        if(member.isEmpty()) return ResponseEntity.notFound().build();

        var getGroupByMemberIdQuery = new GetGroupByMemberIdQuery(member.get().getId());

        var group = this.groupQueryService.handle(getGroupByMemberIdQuery);

        if(group.isEmpty()) return ResponseEntity.notFound().build();

        taskCommandService.handle(new DeleteTasksByMemberId(member.get().getId()));

        var removeMemberFromGroupCommand = new RemoveMemberFromGroupCommand(
                group.get().getId(),
                member.get().getId()
        );

        this.groupCommandService.handle(removeMemberFromGroupCommand);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/next")
    @Operation(summary = "Get the next task by authenticated member", description = "Fetches the next task for the authenticated member.")
    public ResponseEntity<TaskResource> getNextTaskByMemberAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        var getMemberByUsernameQuery = new GetMemberByUsernameQuery(username);

        var member = this.memberQueryService.handle(getMemberByUsernameQuery);

        if(member.isEmpty()) return ResponseEntity.notFound().build();

        var getAllTasksByMemberId = new GetAllTasksByMemberId(member.get().getId());

        var tasks = taskQueryService.handle(getAllTasksByMemberId);

        if (tasks.isEmpty()) return ResponseEntity.notFound().build();

        var inProgressTasks = tasks.stream()
                .filter(task -> task.getStatus().equals(TaskStatus.IN_PROGRESS))
                .collect(Collectors.toList());

        var now = LocalDateTime.now(ZoneId.of("UTC"));

        var nextTask = inProgressTasks.stream()
                .filter(task -> {
                    if (task.getDueDate() == null) {
                        return false;
                    }
                    LocalDateTime dueDate = task.getDueDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    return !dueDate.isBefore(now);
                })
                .min((t1, t2) -> {
                    LocalDateTime d1 = t1.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    LocalDateTime d2 = t2.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    return d1.compareTo(d2);
                });
        if (nextTask.isEmpty()) return ResponseEntity.notFound().build();

        var taskResource = TaskResourceFromEntityAssembler.toResourceFromEntity(nextTask.get());
        return ResponseEntity.ok(taskResource);
    }
}
