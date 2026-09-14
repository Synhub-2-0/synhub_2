package com.hub.synhub_backend.shared.application.external.outboundedservices;

import com.hub.synhub_backend.iam.domain.model.commands.CreateUserMemberCommand;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.interfaces.acl.MemberContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExternalMemberService {
    private final MemberContextFacade memberContextFacade;

    public ExternalMemberService(MemberContextFacade memberContextFacade) {
        this.memberContextFacade = memberContextFacade;
    }


    public Optional<Member> createUserMember(CreateUserMemberCommand command){
        var member = this.memberContextFacade.createMember();
        if(member.isEmpty()) {
            throw new IllegalArgumentException("Error creating member");
        }
        return member;
    }

    public List<Member> getMembersByGroupId(Long groupId){
        var members = this.memberContextFacade.getMemberByGroupId(groupId);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("No members found for group ID: " + groupId);
        }
        return members;
    }

    public Optional<Member> getMemberById(Long memberId) {
        var member = this.memberContextFacade.getMemberById(memberId);
        if (member.isEmpty()) {
            throw new IllegalArgumentException("No member found with ID: " + memberId);
        }
        return member;
    }
}
