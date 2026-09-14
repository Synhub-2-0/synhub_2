package com.hub.synhub_backend.groups.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.RejectInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.queries.GetLeaderByUsernameQuery;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.domain.services.LeaderQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group/invitations")
@Tag(name = "Invitations", description = "Invitation Management Endpoints")
public class LeaderInvitationController {
    private final InvitationCommandService invitationCommandService;
    private final LeaderQueryService leaderQueryService;

    public LeaderInvitationController(InvitationCommandService invitationCommandService, LeaderQueryService leaderQueryService) {
        this.invitationCommandService = invitationCommandService;
        this.leaderQueryService = leaderQueryService;
    }

    @PatchMapping("/{invitationId}")
    @Operation(summary = "Accept or decline an invitation", description = "Accept or decline an invitation for a leader")
    public ResponseEntity<Void> processInvitation(@PathVariable Long invitationId, @AuthenticationPrincipal UserDetails userDetails, @RequestParam(defaultValue = "false") boolean accept) {

        String username = userDetails.getUsername();

        var getLeaderByUsernameQuery = new GetLeaderByUsernameQuery(username);

        var leader = this.leaderQueryService.handle(getLeaderByUsernameQuery);

        if (leader.isEmpty()) return ResponseEntity.notFound().build();

        Long leaderId = leader.get().getId();

        if (accept) {
            var acceptInvitationCommand = new AcceptInvitationCommand(leaderId, invitationId);
            this.invitationCommandService.handle(acceptInvitationCommand);
        } else {
            var cancelInvitationCommand = new RejectInvitationCommand(leaderId, invitationId);
            this.invitationCommandService.handle(cancelInvitationCommand);
        }
        return ResponseEntity.ok().build();
    }
}
