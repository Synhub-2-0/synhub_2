package com.hub.synhub_backend.subscriptions.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.subscriptions.domain.model.commands.ChangePlanCommand;
import com.hub.synhub_backend.subscriptions.domain.model.queries.GetSubscriptionOverviewQuery;
import com.hub.synhub_backend.subscriptions.domain.services.SubscriptionCommandService;
import com.hub.synhub_backend.subscriptions.domain.services.SubscriptionQueryService;
import com.hub.synhub_backend.subscriptions.interfaces.rest.resources.ChangePlanResource;
import com.hub.synhub_backend.subscriptions.interfaces.rest.resources.SubscriptionResource;
import com.hub.synhub_backend.subscriptions.interfaces.rest.transform.SubscriptionResourceFromOverviewAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plan del propio usuario: siempre el de la sesión, nunca el de otro. */
@RestController
@RequestMapping(value = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Subscriptions", description = "Plans and the limit of led groups")
public class SubscriptionController {
    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final UserQueryService userQueryService;

    public SubscriptionController(
            SubscriptionCommandService subscriptionCommandService,
            SubscriptionQueryService subscriptionQueryService,
            UserQueryService userQueryService) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "My plan",
            description = "Current plan, active and locked led groups, free slots and the plan catalog")
    public ResponseEntity<SubscriptionResource> getMySubscription(@AuthenticationPrincipal UserDetails userDetails) {
        var userId = currentUserId(userDetails);
        var overview = subscriptionQueryService.handle(new GetSubscriptionOverviewQuery(userId));
        return ResponseEntity.ok(SubscriptionResourceFromOverviewAssembler.toResourceFromOverview(overview));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Change my plan (simulated payment)",
            description = "Going to a lower limit needs activeGroupIds: exactly the led groups that stay active")
    public ResponseEntity<SubscriptionResource> changeMyPlan(
            @Valid @RequestBody ChangePlanResource resource, @AuthenticationPrincipal UserDetails userDetails) {
        var userId = currentUserId(userDetails);
        subscriptionCommandService.handle(new ChangePlanCommand(userId, resource.plan(), resource.activeGroupIds()));
        var overview = subscriptionQueryService.handle(new GetSubscriptionOverviewQuery(userId));
        return ResponseEntity.ok(SubscriptionResourceFromOverviewAssembler.toResourceFromOverview(overview));
    }

    private Long currentUserId(UserDetails userDetails) {
        return userQueryService
                .handle(new GetUserByUsernameQuery(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}
