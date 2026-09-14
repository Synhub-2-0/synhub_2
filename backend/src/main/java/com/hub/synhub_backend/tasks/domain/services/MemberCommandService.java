package com.hub.synhub_backend.tasks.domain.services;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.domain.model.commands.AddGroupToMemberCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateMemberCommand;

import java.util.Optional;

public interface MemberCommandService {
    Optional<Member> handle(CreateMemberCommand command);
    Optional<Member> handle(AddGroupToMemberCommand command);
}
