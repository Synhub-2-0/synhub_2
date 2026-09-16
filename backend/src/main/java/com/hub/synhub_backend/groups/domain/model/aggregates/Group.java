package com.hub.synhub_backend.groups.domain.model.aggregates;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.valueobjects.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.hub.synhub_backend.groups.domain.model.commands.UpdateGroupCommand;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.ImgUrl;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

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

    @NotNull
    private Integer memberCount;

    @OneToMany(mappedBy = "group")
    private List<GroupProfile> userProfiles;

    private Long leaderId;

    public Group(String name, String description, String imgUrl, User leader, GroupCode code) {
        this.name = name;
        this.imgUrl = new ImgUrl(imgUrl);
        this.description = description;
        this.memberCount = 0;
        this.code = code;

        this.userProfiles.add(new GroupProfile(leader, this, new Role(Roles.ROLE_LEADER)));
        this.leaderId = leader.getId();
    }

    public User getLeader() {
        GroupProfile foundLeaderProfile = this.userProfiles.stream()
                .filter(profile -> profile.getRole().getName() == Roles.ROLE_LEADER)
                .findFirst()
                .orElse(null);
        if (foundLeaderProfile != null) {
            return foundLeaderProfile.getUser();
        }

        return null;
    }

    public void updateInformation(UpdateGroupCommand command) {
        this.name = command.name().isEmpty() ? this.name : command.name();
        this.description = command.description().isEmpty() ? this.description : command.description();
        this.imgUrl = command.imgUrl().isEmpty() ? this.imgUrl : new ImgUrl(command.imgUrl());
    }

    public void removeMember(Long memberId) {
        this.userProfiles.removeIf(member -> member.getId().equals(memberId));
        this.memberCount--;
    }
}
