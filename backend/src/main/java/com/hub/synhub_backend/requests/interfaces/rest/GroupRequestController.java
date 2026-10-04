package com.hub.synhub_backend.requests.interfaces.rest;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.interfaces.rest.resources.RequestResource;
import com.hub.synhub_backend.requests.interfaces.rest.transform.RequestResourceFromEntityAssembler;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1")
@Tag(name = "Group Requests", description = "Group Requests management API")
public class GroupRequestController {

    private final RequestQueryService requestQueryService;
    private final UserQueryService userQueryService;
    private final GroupQueryService groupQueryService;

    public GroupRequestController(RequestQueryService requestQueryService, UserQueryService userQueryService, GroupQueryService groupQueryService) {
        this.requestQueryService = requestQueryService;
        this.userQueryService = userQueryService;
        this.groupQueryService = groupQueryService;
    }

    @GetMapping("/groups/{groupId}/requests")
    @Operation(summary = "Get all requests from a group", description = "Requests of every task in the group, optionally filtered by status and type. Leader only")
    public ResponseEntity<List<RequestResource>> getRequestsByGroup(@PathVariable Long groupId,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String type,
                                                                    @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var group = this.groupQueryService.handle(new GetGroupByIdQuery(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + groupId + " does not exist"));
        if (!group.getLeader().getId().equals(user.getId()))
            throw new AccessDeniedException("Only the group leader can see the group requests");

        var requests = this.requestQueryService.handle(new GetRequestsByGroupIdQuery(groupId, status, type));
        return ResponseEntity.ok(requests.stream().map(RequestResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    // TODO: Get All Requests from a member in a group
}
