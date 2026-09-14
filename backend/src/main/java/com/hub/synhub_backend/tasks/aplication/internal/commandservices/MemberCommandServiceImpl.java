package com.hub.synhub_backend.tasks.aplication.internal.commandservices;

import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.domain.model.commands.AddGroupToMemberCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateMemberCommand;
import com.hub.synhub_backend.tasks.domain.services.MemberCommandService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MemberCommandServiceImpl implements MemberCommandService {
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;

    public MemberCommandServiceImpl(MemberRepository memberRepository, GroupRepository groupRepository) {
        this.memberRepository = memberRepository;
        this.groupRepository = groupRepository;
    }


    @Override
    public Optional<Member> handle(CreateMemberCommand command) {
        var member = new Member(command);
        var createdMember = memberRepository.save(member);
        return Optional.of(createdMember);
    }

    @Override
    public Optional<Member> handle(AddGroupToMemberCommand command) {

        var member = memberRepository.findById(command.memberId());

        if (member.isEmpty()){ throw new RuntimeException("Member not found"); }

        var group = groupRepository.findById(command.groupId());
        if (group.isEmpty()){ throw new RuntimeException("Group not found"); }

        group.get().setMemberCount(group.get().getMemberCount() + 1);
        member.get().setGroup(group.get());

        var updatedMember = memberRepository.save(member.get());
        groupRepository.save(group.get());
        return Optional.of(updatedMember);
    }
}
