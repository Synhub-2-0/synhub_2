package com.hub.synhub_backend.requests.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.requests.domain.model.commands.DeleteRequestCommand;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestByIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByGroupIdQuery;
import com.hub.synhub_backend.requests.domain.model.queries.GetRequestsByTaskIdQuery;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.requests.domain.services.RequestQueryService;
import com.hub.synhub_backend.requests.interfaces.rest.resources.CreateRequestResource;
import com.hub.synhub_backend.requests.interfaces.rest.resources.RequestResource;
import com.hub.synhub_backend.requests.interfaces.rest.transform.CreateRequestCommandFromResourceAssembler;
import com.hub.synhub_backend.requests.interfaces.rest.transform.RequestResourceFromEntityAssembler;
import com.hub.synhub_backend.requests.interfaces.rest.transform.UpdateRequestCommandFromResourceAssembler;
import com.hub.synhub_backend.shared.interfaces.rest.Pagination;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.shared.interfaces.rest.resources.PageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import static com.hub.synhub_backend.shared.interfaces.rest.Pagination.*;

@RestController
@RequestMapping(value = "/api/v1/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Request", description = "Request management API")
public class RequestController {

    private final RequestCommandService requestCommandService;
    private final RequestQueryService requestQueryService;
    private final UserQueryService userQueryService;

    public RequestController(
            RequestCommandService requestCommandService,
            RequestQueryService requestQueryService,
            UserQueryService userQueryService) {
        this.requestCommandService = requestCommandService;
        this.requestQueryService = requestQueryService;
        this.userQueryService = userQueryService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new request",
            description =
                    "Create a request for a task. Only the assigned member; a SUBMISSION needs the task COMPLETED and no pending submission")
    public ResponseEntity<RequestResource> createRequest(
            @Valid @RequestBody CreateRequestResource resource,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var createRequestCommand =
                CreateRequestCommandFromResourceAssembler.toCommandFromResource(resource, user.getId());
        var requestId = this.requestCommandService.handle(createRequestCommand);
        var request = this.requestQueryService
                .handle(new GetRequestByIdQuery(requestId, user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RequestResourceFromEntityAssembler.toResourceFromEntity(request));
    }

    @GetMapping("/task")
    @Operation(summary = "Get requests from a task", description = "Get a list of requests from a task id")
    public ResponseEntity<List<RequestResource>> getRequestsByTaskId(
            @RequestParam Long taskId, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var getRequestsByTaskIdQuery = new GetRequestsByTaskIdQuery(taskId, user.getId());
        var requests = this.requestQueryService.handle(getRequestsByTaskIdQuery);

        var requestResources = requests.stream()
                .map(RequestResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requestResources);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get a request by id", description = "Get a request by id")
    public ResponseEntity<RequestResource> getRequestById(
            @PathVariable Long requestId, @AuthenticationPrincipal UserDetails userDetails
    ) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var getRequestByIdQuery = new GetRequestByIdQuery(requestId, user.getId());
        var optionalRequest = this.requestQueryService.handle(getRequestByIdQuery);

        if (optionalRequest.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var requestResource = RequestResourceFromEntityAssembler.toResourceFromEntity(optionalRequest.get());
        return ResponseEntity.ok(requestResource);
    }

    @GetMapping("/group")
    @Operation(
            summary = "Get all requests from a group."
    )
    public ResponseEntity<?> getRequestsByGroupId(
            @RequestParam Long groupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var query = new GetRequestsByGroupIdQuery(groupId, status, type, user.getId());
        // ?page / ?size: una página (PageResource); sin ellos, el arreglo completo de siempre (AUDIT PERF-02).
        var pageable = of(page, size);
        if (pageable.isPresent())
            return ResponseEntity.ok(PageResource.of(
                    this.requestQueryService.handle(query, pageable.get()),
                    RequestResourceFromEntityAssembler::toResourceFromEntity));
        var requests = this.requestQueryService.handle(query);
        return ResponseEntity.ok(requests.stream()
                .map(RequestResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PutMapping("/{requestId}/status/{status}")
    @Operation(
            summary = "Review a request",
            description =
                    "Approve or reject a request. Leader only; approving a submission marks the task DONE, rejecting sends it back to IN_PROGRESS")
    public ResponseEntity<RequestResource> updateRequestStatus(
            @PathVariable Long requestId,
            @PathVariable String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var updateRequestCommand = UpdateRequestCommandFromResourceAssembler.toCommandFromResource(requestId, status, user.getId());
        var updatedRequest = this.requestCommandService.handle(updateRequestCommand);

        if (updatedRequest.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var requestResource = RequestResourceFromEntityAssembler.toResourceFromEntity(updatedRequest.get());
        return ResponseEntity.ok(requestResource);
    }

    @DeleteMapping("/{requestId}")
    @Operation(summary = "Delete a request by id", description = "Delete a request by id")
    public ResponseEntity<Void> deleteRequestById(@PathVariable Long requestId, @AuthenticationPrincipal UserDetails userDetails) {
        var user = this.userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var deleteRequestCommand = new DeleteRequestCommand(requestId, user.getId());
        this.requestCommandService.handle(deleteRequestCommand);
        return ResponseEntity.noContent().build();
    }
}
