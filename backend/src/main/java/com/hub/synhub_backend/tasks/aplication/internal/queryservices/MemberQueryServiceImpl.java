package com.hub.synhub_backend.tasks.aplication.internal.queryservices;

import com.hub.synhub_backend.shared.application.external.outboundedservices.ExternalIamService;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllMembersQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMemberByIdQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMemberByUsernameQuery;
import com.hub.synhub_backend.tasks.domain.model.queries.GetMembersByGroupIdQuery;
import com.hub.synhub_backend.tasks.domain.services.MemberQueryService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;
    private final ExternalIamService externalIamService;

    public MemberQueryServiceImpl(MemberRepository memberRepository, ExternalIamService externalIamService) {
        this.memberRepository = memberRepository;
        this.externalIamService = externalIamService;
    }

    @Override
    public Optional<Member> handle(GetMemberByIdQuery query) {
        return memberRepository.findById(query.memberId());
    }

    @Override
    public Optional<Member> handle(GetMemberByUsernameQuery query) {
        var user = this.externalIamService.getUserByUsername(query.username());

        var role = user.get().getRoles().stream().findFirst().get().getName().toString();

        if (!role.equals("ROLE_MEMBER")){
            return Optional.empty();
        }

        var member = user.get().getMember();
        if (member == null) {
            return Optional.empty();
        }
        return Optional.of(member);
    }

    @Override
    public List<Member> handle(GetAllMembersQuery query) {
        return this.memberRepository.findAll();
    }

    @Override
    public List<Member> handle(GetMembersByGroupIdQuery query) {
        return memberRepository.findMembersByGroup_Id(query.groupId());
    }
}
