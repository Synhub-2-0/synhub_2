package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Borrado lógico (decisión HUM-03): lo borrado deja de ser visible en toda la API, pero las filas siguen
 * en la base con {@code deleted_at}.
 */
class SoftDeleteApiTests extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    /** null si la fila no existe; si existe, si está marcada como borrada. */
    private Boolean deleted(String table, long id) {
        var rows = jdbc.queryForList("select deleted_at from " + table + " where id = ?", id);
        return rows.isEmpty() ? null : rows.getFirst().get("deleted_at") != null;
    }

    private boolean listContainsId(Res list, long id) {
        for (var item : list.json()) if (item.get("id").asLong() == id) return true;
        return false;
    }

    @Test void deletedTaskAndItsRequestsDisappearButStayInTheDatabase() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);

        assertEquals(204, delete("/tasks/" + taskId, team.leader().token()).status());

        assertEquals(404, get("/tasks/" + taskId, team.leader().token()).status());
        assertFalse(listContainsId(get("/tasks/group/" + team.groupId(), team.leader().token()), taskId));
        assertEquals(0, get("/groups/" + team.groupId() + "/requests", team.leader().token()).json().size());
        assertEquals(Boolean.TRUE, deleted("tasks", taskId));
        assertEquals(Boolean.TRUE, deleted("requests", requestId));
        var profileId = jdbc.queryForObject("select profile_id from tasks where id = ?", Long.class, taskId);
        assertNotNull(profileId, "la tarea borrada conserva a su responsable (historial)");
    }

    @Test void deletedGroupDisappearsWithEverythingInside() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);
        var invitation = post("/invitations", team.leader().token(), Map.of("userId", team.stranger().id(), "groupId", team.groupId()));
        assertEquals(200, invitation.status());

        assertEquals(204, delete("/groups/" + team.groupId(), team.leader().token()).status());

        assertEquals(404, get("/groups/" + team.groupId(), team.leader().token()).status());
        assertEquals(0, get("/groups/user", team.leader().token()).json().size());
        assertEquals(0, get("/groups/user", team.member().token()).json().size());
        assertEquals(0, get("/invitations/user", team.stranger().token()).json().size(), "la invitación pendiente desaparece");
        assertEquals(404, get("/tasks/" + taskId, team.member().token()).status());

        assertEquals(Boolean.TRUE, deleted("groups", team.groupId()));
        assertEquals(Boolean.TRUE, deleted("tasks", taskId));
        assertEquals(Boolean.TRUE, deleted("requests", requestId));
        assertEquals(Boolean.TRUE, deleted("invitations", invitation.id()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from group_profiles where group_id = ? and deleted_at is null", Integer.class, team.groupId()));
    }

    @Test void deletedRequestIsHiddenButKept() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var requestId = submit(team.member(), taskId);

        assertEquals(204, delete("/tasks/" + taskId + "/requests/" + requestId, team.leader().token()).status());

        assertEquals(404, get("/tasks/" + taskId + "/requests/" + requestId, team.leader().token()).status());
        assertEquals(Boolean.TRUE, deleted("requests", requestId));
    }

    @Test void declinedInvitationIsKeptAsHistory() {
        var team = team();
        var invitation = post("/invitations", team.leader().token(), Map.of("userId", team.stranger().id(), "groupId", team.groupId()));
        assertEquals(204, delete("/invitations/" + invitation.id(), team.stranger().token()).status());
        assertEquals(0, get("/invitations/user", team.stranger().token()).json().size());
        assertEquals(Boolean.TRUE, deleted("invitations", invitation.id()));
    }
}
