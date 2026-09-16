package com.hub.synhub_backend.requests.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
@Tag(name = "Group Requests", description = "Group Requests management API")
public class GroupRequestController {

    public GroupRequestController() {
    }

    // TODO: Get All Requests from a group

    // TODO: Get All Requests from a member in a group
}