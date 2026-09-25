package com.hub.synhub_backend.groups.interfaces.rest;

import com.hub.synhub_backend.groups.domain.model.commands.DeleteGroupCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByUserRoleQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateGroupResource;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResourceReduced;
import com.hub.synhub_backend.groups.interfaces.rest.resources.UpdateGroupResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.CreateGroupCommandFromResourceAssembler;
import com.hub.synhub_backend.groups.interfaces.rest.transform.GroupResourceReducedFromEntityAssembler;
import com.hub.synhub_backend.groups.interfaces.rest.transform.UpdateGroupCommandFromResourceAssembler;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByCodeQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.GroupResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.GroupResourceFromEntityAssembler;
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
        var group = this.groupQueryService.handle(getGroupByCodeQuery)
                .orElseThrow(() -> new ResourceNotFoundException("Group with code " + code + " does not exist"));
        var groupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(group);
        return ResponseEntity.ok(groupResource);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by id", description = "Get group by id")
    public ResponseEntity<GroupResource> getGroupById(@PathVariable Long groupId) {
        var getGroupByIdQuery = new GetGroupByIdQuery(groupId);
        var group = this.groupQueryService.handle(getGroupByIdQuery)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + groupId + " does not exist"));
        var groupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(group);
        return ResponseEntity.ok(groupResource);
    }

    @PostMapping()
    @Operation(summary = "Create a new group", description = "Create a new group")
    public ResponseEntity<GroupResource> createGroup(@RequestBody CreateGroupResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var createGroupCommand = CreateGroupCommandFromResourceAssembler.toCommandFromResource(resource, user.getId());
        var newGroup = this.groupCommandService.handle(createGroupCommand);
        var newGroupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(newGroup);
        return ResponseEntity.ok(newGroupResource);
    }

    @GetMapping("/user")
    @Operation(summary = "Get groups by logged user", description = "Get groups by logged user")
    public ResponseEntity<List<GroupResourceReduced>> getGroupsByUserId(@AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var userId = user.getId();
        var getGroupsByIdQuery = new GetGroupsByUserIdQuery(userId);
        var groups = this.groupQueryService.handle(getGroupsByIdQuery);
        var groupResources = groups.stream().map(GroupResourceReducedFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(groupResources);
    }

    @GetMapping("/user/role")
    @Operation(summary = "Get groups by the logged user's role",description = "Get groups by the logged user's role")
    public ResponseEntity<List<GroupResource>> getGroupsByUserRole(@AuthenticationPrincipal UserDetails userDetails, @RequestParam GroupRole groupRole) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var getGroupsByUserRoleQuery = new GetGroupsByUserRoleQuery(user.getId(), groupRole);
        var groups = this.groupQueryService.handle(getGroupsByUserRoleQuery);
        var groupResources = groups.stream().map(GroupResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(groupResources);
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "Updates group details by id")
    public ResponseEntity<GroupResource> updateGroup(@PathVariable Long groupId, @RequestBody UpdateGroupResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var updateGroupCommand = UpdateGroupCommandFromResourceAssembler.toCommandFromResource(resource, groupId, user.getId());
        var group = this.groupCommandService.handle(updateGroupCommand);
        var groupResource = GroupResourceFromEntityAssembler.toResourceFromEntity(group);
        return ResponseEntity.ok(groupResource);
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Deletes a group by id", description = "Deletes a group by id")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        this.groupCommandService.handle(new DeleteGroupCommand(user.getId(), groupId));
        return ResponseEntity.noContent().build();
    }
}
