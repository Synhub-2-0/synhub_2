package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

/** Objetos de dominio reales con ids asignados, para las pruebas de groups. */
final class GroupFixtures {

    private GroupFixtures() {}

    static User user(long id, String username) {
        var user = new User(username, username, "Test", "img", username + "@synhub.io", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        user.setGroupProfiles(new ArrayList<>());
        return user;
    }

    /** Grupo con el líder dado y, opcionalmente, miembros. */
    static Group group(long id, User leader, User... members) {
        var group = new Group("Equipo", "Descripción", "", new GroupCode("ABC123XYZ"));
        ReflectionTestUtils.setField(group, "id", id);
        join(group, leader, GroupRole.GROUP_LEADER);
        for (var member : members) join(group, member, GroupRole.GROUP_MEMBER);
        return group;
    }

    static GroupProfile join(Group group, User user, GroupRole role) {
        var profile = new GroupProfile(user, group, role);
        group.getUserProfiles().add(profile);
        user.getGroupProfiles().add(profile);
        return profile;
    }

    static Invitation invitation(long id, User user, Group group) {
        var invitation = new Invitation(user, group);
        ReflectionTestUtils.setField(invitation, "id", id);
        return invitation;
    }
}
