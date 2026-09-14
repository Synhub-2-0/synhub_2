package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.LeaderRepository;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GroupCommandServiceImpl implements GroupCommandService {

    private final GroupRepository groupRepository;
    private final LeaderRepository leaderRepository;
    private final MemberRepository memberRepository;

    public GroupCommandServiceImpl(GroupRepository groupRepository, LeaderRepository leaderRepository, MemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.leaderRepository = leaderRepository;
        this.memberRepository = memberRepository;
    }


    @Override
    public Optional<Group> handle(CreateGroupCommand command) {

        Leader leader = leaderRepository.findById(command.leaderId()).get();

        Group group = new Group(
                command.name(),
                command.description(),
                command.imgUrl(),
                leader,
                GroupCode.random()
                );
        while( groupRepository.existsByCode(group.getCode())) {
            group.setCode(GroupCode.random());
        }

        groupRepository.save(group);

        return Optional.of(group);
    }

    @Override
    public Optional<Group> handle(UpdateGroupCommand command) {
        var group = groupRepository.findByLeader_Id(command.leaderId()).get();
        var groupId = group.getId();
        if(!this.groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
        }

        var groupToUpdate = groupRepository.findById(groupId).get();
        groupToUpdate.updateInformation(command);

        try{
            var updatedGroup = groupRepository.save(groupToUpdate);
            return Optional.of(updatedGroup);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while updating group: " + e.getMessage());
        }
    }

    @Override
    public void handle(DeleteGroupCommand command) {

        var groupId = groupRepository.findByLeader_Id(command.leaderId()).get().getId();

        var group = groupRepository.findById(groupId);

        if(!this.groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group with id " + groupId + " does not exist");
        }
        try {
            groupRepository.delete(group.get());
        }catch (Exception e) {
            throw new IllegalArgumentException("Error while deleting group: " + e.getMessage());
        }
    }

    @Override
    public void handle(RemoveMemberFromGroupCommand command) {
        var groupId = groupRepository.findByLeader_Id(command.leaderId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found for leader"))
                .getId();

        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group with id " + groupId + " does not exist"));

        var member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new IllegalArgumentException("Member with id " + command.memberId() + " does not exist"));

        if (!group.getMembers().contains(member)) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist in group with id " + groupId);
        }

        try {
            group.getMembers().remove(member);
            member.setGroup(null);
            group.setMemberCount(group.getMembers().size());
            memberRepository.save(member);
            groupRepository.save(group);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while removing member from group: " + e.getMessage());
        }
    }

    @Override
    public void handle(LeaveGroupCommand command) {
        var group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found for member"));

        var member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new IllegalArgumentException("Member with id " + command.memberId() + " does not exist"));

        if (!group.getMembers().contains(member)) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist in group with id " + group.getId());
        }

        try {
            group.getMembers().remove(member);
            member.setGroup(null);
            group.setMemberCount(group.getMembers().size());
            memberRepository.save(member);
            groupRepository.save(group);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while removing member from group: " + e.getMessage());
        }
    }
}
