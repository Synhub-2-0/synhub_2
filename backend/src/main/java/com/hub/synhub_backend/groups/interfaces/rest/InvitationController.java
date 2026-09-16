package com.hub.synhub_backend.groups.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations", description = "Invitation Managements Endpoints")
public class InvitationController {
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    public InvitationController() {

    }

    // TODO: Create an invitation

    // TODO: Get Invitations By Group

    // TODO: Cancel an Invitation

    // TODO: Get All Invitations from a Member
}
