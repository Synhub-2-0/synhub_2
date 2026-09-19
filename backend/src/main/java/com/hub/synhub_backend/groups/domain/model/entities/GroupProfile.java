package com.hub.synhub_backend.groups.domain.model.entities;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.shared.domain.model.entities.AuditableModel;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
public class GroupProfile extends AuditableModel {

    @ManyToOne()
    @JoinColumn(name = "user_id")
    @Getter
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    @Getter
    private Group group;

    @Getter
    private GroupRole roleInGroup;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Task> tasks;

    public GroupProfile() {
        this.user = null;
        this.group = null;
        this.roleInGroup = null;
        this.tasks = null;
    }

    public GroupProfile(User user, Group group, GroupRole roleInGroup) {
        this.user = user;
        this.group = group;
        this.roleInGroup = roleInGroup;
    }

    public void addTask(Task task) {
        this.tasks.add(task);
        task.setProfile(this);
    }

    public void removeTask(Task task) {
        this.tasks.remove(task);
        task.setProfile(null);
    }
}
