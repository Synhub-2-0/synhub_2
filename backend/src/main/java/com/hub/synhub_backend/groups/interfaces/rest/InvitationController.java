package com.hub.synhub_backend.groups.interfaces.rest;

import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.domain.services.InvitationQueryService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateInvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.CreateInvitationCommandFromResourceAssembler;
import com.hub.synhub_backend.groups.interfaces.rest.transform.InvitationResourceFromEntityAssembler;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
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
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);
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
    public ResponseEntity<InvitationResource> createInvitation(@RequestBody CreateInvitationResource resource) {
        var createInvitationCommand = CreateInvitationCommandFromResourceAssembler.toCommandFromResource(resource);
        var invitationOpt = invitationCommandService.handle(createInvitationCommand);
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var invitation = invitationOpt.get();
        var invitationResource = InvitationResourceFromEntityAssembler.toResourceFromEntity(invitation);
        return ResponseEntity.ok(invitationResource);
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invitation and join user to the group", description = "Accept an invitation and join user to the group")
    public ResponseEntity<Void> acceptInvitation(@RequestParam Long invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var user = userOpt.get();

        var acceptInvitationCommand = new AcceptInvitationCommand(user.getId(), invitationId);

        try {
            this.invitationCommandService.handle(acceptInvitationCommand);
        } catch (Error error) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/user")
    @Operation(summary = "Gets invitations towards the logged user", description = "Gets invitations towards the logged user")
    public ResponseEntity<List<InvitationResource>> getUserInvitations(@AuthenticationPrincipal UserDetails userDetails) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(userDetails.getUsername());
        var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var user = userOpt.get();
        var getInvitationsByUserIdQuery = new GetInvitationsByUserIdQuery(user.getId());
        var invitations = this.invitationQueryService.handle(getInvitationsByUserIdQuery);
        var invitationResources = invitations.stream().map(InvitationResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(invitationResources);
    }

    // TODO: Get Invitations By Group

    // TODO: Cancel an Invitation
}
