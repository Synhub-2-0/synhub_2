package com.hub.synhub_backend.tasks.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateMemberCommand;

import java.util.List;

@Getter
@Setter
@Entity
public class Member extends AuditableAbstractAggregateRoot<Member> {

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @OneToMany(mappedBy = "member")
    private List<Task> tasks;

    @OneToOne(mappedBy = "member")
    private User user;

    public Member() {}

    public Member(CreateMemberCommand command) {
    }

    public void addTask(Task task) {
        this.tasks.add(task);
        task.setMember(this);
    }

    public void removeTask(Task task) {
        this.tasks.remove(task);
        task.setMember(null);
    }
}
