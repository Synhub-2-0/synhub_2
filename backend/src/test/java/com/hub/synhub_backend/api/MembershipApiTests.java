package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unirse por código y quitar integrantes (decisión HUM-05). */
class MembershipApiTests extends ApiTestSupport {

    private String codeOf(Account leader, long groupId) {
        return get("/groups/" + groupId, leader.token()).text("code");
    }

    @Test
    void joinByCodeMakesTheUserAMember() {
        var leader = account("lider");
        var newcomer = account("nuevo");
        var groupId = createGroup(leader);
        var invitation = post("/invitations", leader.token(), Map.of("userId", newcomer.id(), "groupId", groupId));

        var joined = post(
                "/groups/join",
                newcomer.token(),
                Map.of("code", codeOf(leader, groupId).toLowerCase()));

        assertEquals(200, joined.status(), joined.raw());
        assertEquals(groupId, joined.id());
        assertEquals(200, get("/groups/" + groupId, newcomer.token()).status());
        assertEquals(
                2, get("/groups/" + groupId + "/members", leader.token()).json().size());
        assertEquals(0, get("/invitations/user", newcomer.token()).json().size(), "la invitación pendiente se consume");
        assertEquals(
                404, delete("/invitations/" + invitation.id(), leader.token()).status());
    }

    @Test
    void joinByCodeErrors() {
        var team = team();
        var code = codeOf(team.leader(), team.groupId());
        assertEquals(
                409,
                post("/groups/join", team.member().token(), Map.of("code", code))
                        .status(),
                "ya es miembro");
        assertEquals(
                409,
                post("/groups/join", team.leader().token(), Map.of("code", code))
                        .status(),
                "ya es líder");
        assertEquals(
                404,
                post("/groups/join", team.stranger().token(), Map.of("code", "ZZZZZZZZZ"))
                        .status());
        assertEquals(
                400,
                post("/groups/join", team.stranger().token(), Map.of("code", "corto"))
                        .status());
        assertEquals(
                400,
                post("/groups/join", team.stranger().token(), Map.of("code", ""))
                        .status());
    }

    @Test
    void leaderRemovesAMemberAndTheirTasksDisappear() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());

        assertEquals(
                204,
                delete(
                                "/groups/" + team.groupId() + "/members/"
                                        + team.member().id(),
                                team.leader().token())
                        .status());

        assertEquals(
                1,
                get("/groups/" + team.groupId() + "/members", team.leader().token())
                        .json()
                        .size());
        assertEquals(
                403, get("/groups/" + team.groupId(), team.member().token()).status(), "ya no es integrante");
        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status(), "sus tareas se borran");
        assertEquals(0, get("/groups/user", team.member().token()).json().size());
    }

    @Test
    void memberCanLeaveAndJoinAgain() {
        var team = team();
        var code = codeOf(team.leader(), team.groupId());
        assertEquals(
                204,
                delete(
                                "/groups/" + team.groupId() + "/members/"
                                        + team.member().id(),
                                team.member().token())
                        .status());
        assertEquals(
                200,
                post("/groups/join", team.member().token(), Map.of("code", code))
                        .status(),
                "puede volver a unirse");
        assertEquals(
                2,
                get("/groups/" + team.groupId() + "/members", team.leader().token())
                        .json()
                        .size());
    }

    @Test
    void removalPermissions() {
        var team = team();
        var other = account("otro");
        join(team.leader(), other, team.groupId());
        var members = "/groups/" + team.groupId() + "/members/";

        assertEquals(403, delete(members + other.id(), team.member().token()).status(), "un miembro no expulsa a otro");
        assertEquals(
                403,
                delete(members + team.member().id(), team.stranger().token()).status());
        assertEquals(
                409,
                delete(members + team.leader().id(), team.leader().token()).status(),
                "AUDIT BUG-10: el líder no sale (409)");
        assertEquals(
                404,
                delete(members + team.stranger().id(), team.leader().token()).status(),
                "no es integrante");
    }
}
