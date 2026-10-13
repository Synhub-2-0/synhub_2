package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.UpdateGroupCommand;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.model.valueobjects.ImgUrl;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import org.junit.jupiter.api.Test;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Agregado Group, value objects y membresía. */
class GroupDomainTest {

    // ---- GroupCode
    @Test void randomCodeHasNineUppercaseAlphanumerics() {
        for (int i = 0; i < 50; i++) {
            assertTrue(GroupCode.random().code().matches("[0-9A-Z]{9}"));
        }
    }

    @Test void invalidCodesAreRejected() {
        assertDoesNotThrow(() -> new GroupCode("ABC123XYZ"));
        assertThrows(IllegalArgumentException.class, () -> new GroupCode(null));
        assertThrows(IllegalArgumentException.class, () -> new GroupCode("ABC"));
        assertThrows(IllegalArgumentException.class, () -> new GroupCode("abc123xyz"));
        assertThrows(IllegalArgumentException.class, () -> new GroupCode("ABC-23XYZ"));
    }

    // ---- ImgUrl
    @Test void blankImageBecomesEmpty() {
        assertEquals("", new ImgUrl(null).imgUrl());
        assertEquals("", new ImgUrl("   ").imgUrl());
        assertEquals("https://x/y.png", new ImgUrl("https://x/y.png").imgUrl());
    }

    // ---- Group
    @Test void leaderAndMemberCountComeFromMemberships() {
        var leader = user(1, "lider");
        var group = group(10, leader, user(2, "ana"), user(3, "luis"));
        assertSame(leader, group.getLeader());
        assertEquals(3, group.getMemberCount());
    }

    @Test void groupWithoutLeaderReturnsNull() {
        assertNull(new Group("G", "d", "", GroupCode.random()).getLeader());
    }

    @Test void updateInformationIgnoresEmptyFields() {
        var group = group(10, user(1, "lider"));
        group.updateInformation(new UpdateGroupCommand(1L, 10L, "Nuevo", "", ""));
        assertEquals("Nuevo", group.getName());
        assertEquals("Descripción", group.getDescription());

        group.updateInformation(new UpdateGroupCommand(1L, 10L, "", "Otra", "https://img"));
        assertEquals("Nuevo", group.getName());
        assertEquals("Otra", group.getDescription());
        assertEquals("https://img", group.getImgUrl().imgUrl());
    }

    // ---- GroupProfile
    // AUDIT BUG-12 (resuelto)
    @Test void newMembershipAcceptsTasks() {
        var profile = new GroupProfile(user(2, "ana"), group(10, user(1, "lider")), GroupRole.GROUP_MEMBER);
        var task = new Task();
        assertDoesNotThrow(() -> profile.addTask(task));
        assertSame(profile, task.getProfile());
    }

    @Test void updateInformationIgnoresNullFields() {
        var group = group(10, user(1, "lider"));
        group.updateInformation(new UpdateGroupCommand(1L, 10L, null, null, null));
        assertEquals("Equipo", group.getName());
        assertEquals("Descripción", group.getDescription());
    }
}
