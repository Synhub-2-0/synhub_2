package com.hub.synhub_backend.groups.interfaces.rest;

import com.hub.synhub_backend.groups.domain.model.commands.DeleteGroupCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateGroupResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.CreateGroupCommandFromResourceAssembler;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByCodeQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.GroupResourceFromEntityAssembler;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/groups")
@Tag(name = "Groups", description = "Group management API")
public class GroupController {
    private final GroupQueryService groupQueryService;
    private final GroupCommandService groupCommandService;
    private final UserQueryService userQueryService;

    public GroupController(
            GroupQueryService groupQueryService,
            UserQueryService userQueryService,
            GroupCommandService groupCommandService
            ) {
        this.groupQueryService = groupQueryService;
        this.userQueryService = userQueryService;
        this.groupCommandService = groupCommandService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search for a group by code", description = "Search for a group by code")
    public ResponseEntity<GroupResource> searchGroupByCode(@RequestParam String code) {
        var getGroupByCodeQuery = new GetGroupByCodeQuery(code);
        var group = this.groupQueryService.handle(getGroupByCodeQuery);
        if (group.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var groupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(group.get());
        return ResponseEntity.ok(groupResource);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by id", description = "Get group by id")
    public ResponseEntity<GroupResource> getGroupById(@PathVariable Long groupId) {
        var getGroupByIdQuery = new GetGroupByIdQuery(groupId);

        var groupOpt = this.groupQueryService.handle(getGroupByIdQuery);

        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var group = groupOpt.get();

        var groupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(group);

        return ResponseEntity.ok(groupResource);
    }

    @PostMapping()
    @Operation(summary = "Create a new group", description = "Create a new group")
    public ResponseEntity<GroupResource> createGroup(@RequestBody CreateGroupResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var user = userOpt.get();

        var createGroupCommand = CreateGroupCommandFromResourceAssembler.toCommandFromResource(resource, user.getId());

        var newGroupOpt = this.groupCommandService.handle(createGroupCommand);
        if (newGroupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var newGroup = newGroupOpt.get();

        var newGroupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(newGroup);

        return ResponseEntity.ok(newGroupResource);
    }

    @GetMapping("/user")
    @Operation(summary = "Get groups by logged user", description = "Get groups by logged user")
    public ResponseEntity<List<GroupResource>> getGroupsByUserId(@AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var user = userOpt.get();
        var userId = user.getId();

        var getGroupsByIdQuery = new GetGroupsByUserIdQuery(userId);
        var groups = this.groupQueryService.handle(getGroupsByIdQuery);
        var groupResources = groups.stream().map(GroupResourceFromEntityAssembler::toResourceFromEntity).toList();

        return ResponseEntity.ok(groupResources);
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group by id", description = "Delete a group by id")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var user = userOpt.get();

        var getGroupByIdQuery = new GetGroupByIdQuery(groupId);
        var groupOpt = this.groupQueryService.handle(getGroupByIdQuery);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var group = groupOpt.get();

        var leader = group.getLeader();
        if (leader == null || !leader.getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var deleteGroupCommand = new DeleteGroupCommand(groupId);
        this.groupCommandService.handle(deleteGroupCommand);

        return ResponseEntity.noContent().build();
    }
}
