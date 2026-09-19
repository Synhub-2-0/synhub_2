package com.hub.synhub_backend.groups.interfaces.rest;

import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.interfaces.rest.resources.CreateInvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.resources.InvitationResource;
import com.hub.synhub_backend.groups.interfaces.rest.transform.CreateInvitationCommandFromResourceAssembler;
import com.hub.synhub_backend.groups.interfaces.rest.transform.InvitationResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations", description = "Invitation Managements Endpoints")
public class InvitationController {
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);
    private final InvitationCommandService invitationCommandService;

    public InvitationController(
            InvitationCommandService invitationCommandService
    ) {
        this.invitationCommandService = invitationCommandService;
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

    // TODO: Get Invitations By Group

    // TODO: Cancel an Invitation

    // TODO: Get All Invitations from a User
}
