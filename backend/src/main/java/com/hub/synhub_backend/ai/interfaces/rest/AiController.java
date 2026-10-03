package com.hub.synhub_backend.ai.interfaces.rest;

import com.hub.synhub_backend.ai.domain.services.TaskClassificationQueryService;
import com.hub.synhub_backend.ai.interfaces.rest.resources.ClassifyTaskResource;
import com.hub.synhub_backend.ai.interfaces.rest.resources.TaskClassificationResource;
import com.hub.synhub_backend.ai.interfaces.rest.transform.ClassifyTaskQueryFromResourceAssembler;
import com.hub.synhub_backend.ai.interfaces.rest.transform.TaskClassificationResourceFromValueObjectAssembler;
import com.hub.synhub_backend.ai.domain.model.queries.GenerateGroupReportQuery;
import com.hub.synhub_backend.ai.domain.services.GroupReportQueryService;
import com.hub.synhub_backend.ai.interfaces.rest.resources.GroupReportResource;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/ai")
@Tag(name = "AI", description = "AI assistant API")
public class AiController {

    private final TaskClassificationQueryService taskClassificationQueryService;

    private final GroupReportQueryService groupReportQueryService;
    private final UserQueryService userQueryService;
    private final GroupQueryService groupQueryService;

    public AiController(TaskClassificationQueryService taskClassificationQueryService,
                        GroupReportQueryService groupReportQueryService,
                        UserQueryService userQueryService, GroupQueryService groupQueryService) {
        this.taskClassificationQueryService = taskClassificationQueryService;
        this.groupReportQueryService = groupReportQueryService;
        this.userQueryService = userQueryService;
        this.groupQueryService = groupQueryService;
    }

    @GetMapping("/groups/{groupId}/report")
    @Operation(summary = "AI group report", description = "Narrative report of a group generated from its metrics. Leader only")
    public ResponseEntity<GroupReportResource> getGroupReport(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails userDetails) {
        var user = userQueryService.handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var group = groupQueryService.handle(new GetGroupByIdQuery(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!group.getLeader().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the leader can generate the group report");
        }

        var report = groupReportQueryService.handle(new GenerateGroupReportQuery(groupId));
        if (report.isEmpty()) return ResponseEntity.status(503).build();
        return ResponseEntity.ok(new GroupReportResource(report.get()));
    }

    @PostMapping("/classify")
    @Operation(summary = "Classify a task", description = "Suggests labels, urgency, difficulty and context for a task")
    public ResponseEntity<TaskClassificationResource> classifyTask(@RequestBody ClassifyTaskResource resource) {
        if (resource.title() == null || resource.title().isBlank()) return ResponseEntity.badRequest().build();

        var classification = taskClassificationQueryService.handle(
                ClassifyTaskQueryFromResourceAssembler.toQueryFromResource(resource));
        if (classification.isEmpty()) return ResponseEntity.status(503).build();

        return ResponseEntity.ok(TaskClassificationResourceFromValueObjectAssembler.toResourceFromValueObject(classification.get()));
    }
}
