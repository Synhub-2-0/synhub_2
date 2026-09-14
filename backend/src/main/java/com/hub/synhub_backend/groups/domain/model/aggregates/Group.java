package com.hub.synhub_backend.groups.domain.model.aggregates;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.hub.synhub_backend.groups.domain.model.commands.UpdateGroupCommand;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.ImgUrl;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

import java.util.List;

@Entity
@NoArgsConstructor
@Setter
@Getter
public class Group extends AuditableAbstractAggregateRoot<Group> {

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "code", unique = true))
    private GroupCode code;

    @NotNull
    private String name;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String description;

    @Embedded
    private ImgUrl imgUrl;

    @OneToOne
    @JoinColumn(name = "leader_id")
    private Leader leader;

    @NotNull
    private Integer memberCount;

    @OneToMany(mappedBy = "group")
    private List<Member> members;

    public Group(String name, String description, String imgUrl , Leader leader, GroupCode code) {
        this.name = name;
        this.imgUrl = new ImgUrl(imgUrl);
        this.leader = leader;
        this.description = description;
        this.memberCount = 0;
        this.code = code;
    }

    public void updateInformation(UpdateGroupCommand command) {
        this.name = command.name().isEmpty() ? this.name : command.name();
        this.description = command.description().isEmpty() ? this.description : command.description();
        this.imgUrl = command.imgUrl().isEmpty() ? this.imgUrl : new ImgUrl(command.imgUrl());
    }

    public void removeMember(Long memberId) {
        this.members.removeIf(member -> member.getId().equals(memberId));
        this.memberCount--;
    }
}
