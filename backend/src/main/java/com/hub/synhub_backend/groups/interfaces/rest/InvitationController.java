package com.hub.synhub_backend.groups.interfaces.rest;

import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.domain.services.InvitationQueryService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateInvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.CreateInvitationCommandFromResourceAssembler;
import com.hub.synhub_backend.groups.interfaces.rest.transform.InvitationResourceFromEntityAssembler;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations", description = "Invitation Managements Endpoints")
public class InvitationController {
    private final InvitationCommandService invitationCommandService;
    private final InvitationQueryService invitationQueryService;
    private final UserQueryService userQueryService;

    public InvitationController(
            InvitationCommandService invitationCommandService,
            UserQueryService userQueryService,
            InvitationQueryService invitationQueryService
    ) {
        this.invitationCommandService = invitationCommandService;
        this.userQueryService = userQueryService;
        this.invitationQueryService = invitationQueryService;
    }

    @PostMapping()
    @Operation(summary = "Creates an invitation to a group for a user")
    public ResponseEntity<InvitationResource> createInvitation(@RequestBody CreateInvitationResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var user = this.userQueryService.handle(getUserByUsernameQuery)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var createInvitationCommand = CreateInvitationCommandFromResourceAssembler.toCommandFromResource(user.getId(), resource);
        var invitation = invitationCommandService.handle(createInvitationCommand);
        var invitationResource = InvitationResourceFromEntityAssembler.toResourceFromEntity(invitation);
        return ResponseEntity.ok(invitationResource);
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invitation to join a group", description = "Accept an invitation to join a group")
    public ResponseEntity<Void> acceptInvitation(@RequestParam Long invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var user = this.userQueryService.handle(getUserByUsernameQuery)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var acceptInvitationCommand = new AcceptInvitationCommand(user.getId(), invitationId);
        this.invitationCommandService.handle(acceptInvitationCommand);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{invitationId}")
    @Operation(summary = "Cancels an invitation by id", description = "Cancels an invitation by id")
    public ResponseEntity<Void> cancelInvitation(@PathVariable Long invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var user = this.userQueryService.handle(getUserByUsernameQuery)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var cancelInvitationCommand = new CancelInvitationCommand(user.getId(), invitationId);
        this.invitationCommandService.handle(cancelInvitationCommand);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    @Operation(summary = "Gets invitations towards the logged user", description = "Gets invitations towards the logged user")
    public ResponseEntity<List<InvitationResource>> getUserInvitations(@AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var user = this.userQueryService.handle(getUserByUsernameQuery)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var getInvitationsByUserIdQuery = new GetInvitationsByUserIdQuery(user.getId());
        var invitations = this.invitationQueryService.handle(getInvitationsByUserIdQuery);
        var invitationResources = invitations.stream().map(InvitationResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(invitationResources);
    }

    @GetMapping("/group")
    @Operation(summary = "Gets invitations by group id", description = "Gets invitations by group id")
    public ResponseEntity<List<InvitationResource>> getInvitationsByGroup(@RequestParam Long groupId) {
        var getInvitationsByGroupIdQuery = new GetInvitationsByGroupIdQuery(groupId);
        var invitations = this.invitationQueryService.handle(getInvitationsByGroupIdQuery);
        var invitationResources = invitations.stream().map(InvitationResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(invitationResources);
    }
}
