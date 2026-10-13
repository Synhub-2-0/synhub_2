package com.hub.synhub_backend.requests.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.interfaces.rest.transform.RequestResourceFromEntityAssembler;
import com.hub.synhub_backend.shared.interfaces.rest.Pagination;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.shared.interfaces.rest.resources.PageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1")
@Tag(name = "Group Requests", description = "Group Requests management API")
public class GroupRequestController {

    private final RequestQueryService requestQueryService;
    private final UserQueryService userQueryService;

    public GroupRequestController(RequestQueryService requestQueryService, UserQueryService userQueryService) {
        this.requestQueryService = requestQueryService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/groups/{groupId}/requests")
    @Operation(
            summary = "Get all requests from a group",
            description =
                    "Requests of every task in the group, newest first, optionally filtered by status and type. Leader only")
    public ResponseEntity<?> getRequestsByGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var query = new GetRequestsByGroupIdQuery(groupId, status, type, user.getId());
        // ?page / ?size: una página (PageResource); sin ellos, el arreglo completo de siempre (AUDIT PERF-02).
        var pageable = Pagination.of(page, size);
        if (pageable.isPresent())
            return ResponseEntity.ok(PageResource.of(
                    this.requestQueryService.handle(query, pageable.get()),
                    RequestResourceFromEntityAssembler::toResourceFromEntity));
        var requests = this.requestQueryService.handle(query);
        return ResponseEntity.ok(requests.stream()
                .map(RequestResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    // TODO: Get All Requests from a member in a group
}
