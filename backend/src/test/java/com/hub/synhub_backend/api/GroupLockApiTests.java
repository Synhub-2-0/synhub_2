package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Grupos bloqueados por el límite del plan: se crean bloqueados y su líder los activa cuando hay cupo. */
class GroupLockApiTests extends ApiTestSupport {

    private boolean locked(long groupId, Account who) {
        return get("/groups/" + groupId, who.token()).json().get("locked").asBoolean();
    }

    /** Un grupo con integrante, tareas y una entrega pendiente que queda bloqueado al bajar de plan. */
    private record LockedScenario(
            Team team, long groupId, long inProgressTaskId, long pendingRequestId, long keptA, long keptB) {}

    private LockedScenario lockedScenario() {
        var team = new Team(account("lider"), account("miembro"), account("ajeno"), 0);
        var leader = team.leader();
        assertEquals(
                200,
                put("/subscriptions/me", leader.token(), java.util.Map.of("plan", "PRO"))
                        .status());
        var groupId = createGroup(leader);
        join(leader, team.member(), groupId);
        var inProgress = createTask(leader, groupId, team.member());
        var submitted = createTask(leader, groupId, team.member());
        var requestId = submit(team.member(), submitted);
        var keptA = createGroup(leader);
        var keptB = createGroup(leader);
        // Baja a Gratis conservando solo estos dos: el primero (con el equipo) queda bloqueado.
        var downgraded = put(
                "/subscriptions/me",
                leader.token(),
                java.util.Map.of("plan", "FREE", "activeGroupIds", java.util.List.of(keptA, keptB)));
        assertEquals(200, downgraded.status(), downgraded.raw());
        assertTrue(locked(groupId, leader));
        return new LockedScenario(team, groupId, inProgress, requestId, keptA, keptB);
    }

    @Test
    void aLockedGroupRejectsEveryWriteForEveryone() {
        var s = lockedScenario();
        var leader = s.team().leader();
        var member = s.team().member();
        var guest = account("invitado");
        var code = get("/groups/" + s.groupId(), leader.token()).text("code");
        var token = leader.token();

        // Groups e invitaciones
        assertEquals(
                409,
                put("/groups/" + s.groupId(), token, java.util.Map.of("name", "Nuevo nombre"))
                        .status());
        assertEquals(
                409,
                post("/invitations", token, java.util.Map.of("userId", guest.id(), "groupId", s.groupId()))
                        .status());
        assertEquals(
                409,
                post("/groups/join", guest.token(), java.util.Map.of("code", code))
                        .status());
        assertEquals(
                409,
                delete("/groups/" + s.groupId() + "/members/" + member.id(), token)
                        .status());

        // Tareas
        assertEquals(
                409,
                post("/tasks", token, taskBody(s.groupId(), member.id(), "Nueva tarea"))
                        .status());
        assertEquals(
                409,
                put("/tasks/" + s.inProgressTaskId(), token, java.util.Map.of("title", "Otro", "userId", member.id()))
                        .status());
        assertEquals(
                409,
                put("/tasks/" + s.inProgressTaskId() + "/status?status=ON_HOLD", token, null)
                        .status());
        assertEquals(409, delete("/tasks/" + s.inProgressTaskId(), token).status());

        // Solicitudes: el integrante no entrega ni pide modificaciones; el líder no revisa ni borra
        assertEquals(
                409,
                post(
                                "/requests",
                                member.token(),
                                java.util.Map.of(
                                        "taskId",
                                        s.inProgressTaskId(),
                                        "description",
                                        "Listo",
                                        "requestType",
                                        "SUBMISSION"))
                        .status());
        assertEquals(
                409,
                post(
                                "/requests",
                                member.token(),
                                java.util.Map.of(
                                        "taskId",
                                        s.inProgressTaskId(),
                                        "description",
                                        "Cambiar",
                                        "requestType",
                                        "MODIFICATION"))
                        .status());
        assertEquals(
                409,
                put("/requests/" + s.pendingRequestId() + "/status/APPROVED", token, null)
                        .status());
        assertEquals(409, delete("/requests/" + s.pendingRequestId(), token).status());

        // Informe de IA (cuesta una llamada a Bedrock)
        assertEquals(409, get("/ai/groups/" + s.groupId() + "/report", token).status());

        // Nada cambió
        assertEquals("IN_PROGRESS", get("/tasks/" + s.inProgressTaskId(), token).text("status"));
        assertEquals("PENDING", get("/requests/" + s.pendingRequestId(), token).text("requestStatus"));
    }

    @Test
    void aLockedGroupStaysReadableAndItsPeopleCanLeaveOrDeleteIt() {
        var s = lockedScenario();
        var leader = s.team().leader();
        var member = s.team().member();

        // Se puede ver todo, con o sin ser el líder
        for (var who : java.util.List.of(leader, member)) {
            assertEquals(200, get("/groups/" + s.groupId(), who.token()).status());
            assertEquals(
                    200, get("/groups/" + s.groupId() + "/members", who.token()).status());
            assertEquals(200, get("/tasks/group/" + s.groupId(), who.token()).status());
            assertEquals(200, get("/tasks/" + s.inProgressTaskId(), who.token()).status());
            assertEquals(
                    200,
                    get("/requests/task?taskId=" + s.inProgressTaskId(), who.token())
                            .status());
            assertEquals(
                    200,
                    get("/metrics/groups/" + s.groupId() + "/overview", who.token())
                            .status());
        }
        assertEquals(
                200,
                get("/requests/group?groupId=" + s.groupId(), leader.token()).status());
        assertTrue(get("/groups/user", member.token()).raw().contains("\"locked\":true"));

        // El integrante puede salir; el líder puede borrar el grupo
        assertEquals(
                204,
                delete("/groups/" + s.groupId() + "/members/" + member.id(), member.token())
                        .status());
        assertEquals(204, delete("/groups/" + s.groupId(), leader.token()).status());
    }

    @Test
    void activatingALockedGroupReturnsItsWorkToNormal() {
        var s = lockedScenario();
        var leader = s.team().leader();
        var member = s.team().member();

        // Sin cupo no se activa; al borrar uno de los activos, sí
        assertEquals(
                409,
                post("/groups/" + s.groupId() + "/activate", leader.token(), null)
                        .status());
        assertEquals(204, delete("/groups/" + s.keptB(), leader.token()).status());
        assertEquals(
                200,
                post("/groups/" + s.groupId() + "/activate", leader.token(), null)
                        .status());

        assertFalse(locked(s.groupId(), leader));
        assertEquals(
                200,
                post("/tasks", leader.token(), taskBody(s.groupId(), member.id(), "Tarea nueva"))
                        .status());
        assertEquals(
                200,
                put("/requests/" + s.pendingRequestId() + "/status/APPROVED", leader.token(), null)
                        .status());
    }

    @Test
    void theThirdGroupOfAFreeLeaderIsCreatedLocked() {
        var leader = account("lider");
        var first = createGroup(leader);
        var second = createGroup(leader);
        var third = createGroup(leader);

        assertFalse(locked(first, leader));
        assertFalse(locked(second, leader));
        assertTrue(locked(third, leader), "el límite del plan Gratis es 2 grupos activos");
    }

    @Test
    void theLeaderActivatesALockedGroupOnlyWhenAnActiveOneIsDeleted() {
        var leader = account("lider");
        var first = createGroup(leader);
        createGroup(leader);
        var third = createGroup(leader);

        assertEquals(
                409,
                post("/groups/" + third + "/activate", leader.token(), null).status(),
                "sin cupo no se activa");
        assertTrue(locked(third, leader));

        assertEquals(204, delete("/groups/" + first, leader.token()).status());
        var activated = post("/groups/" + third + "/activate", leader.token(), null);

        assertEquals(200, activated.status(), activated.raw());
        assertFalse(activated.json().get("locked").asBoolean());
        assertFalse(locked(third, leader));
    }

    @Test
    void anActiveGroupCannotBeActivated() {
        var leader = account("lider");
        var first = createGroup(leader);
        assertEquals(
                409,
                post("/groups/" + first + "/activate", leader.token(), null).status());
    }

    @Test
    void onlyTheLeaderOfTheGroupActivatesIt() {
        var team = team();
        var leader = team.leader();
        createGroup(leader);
        var locked = createGroup(leader); // tercero (team() ya creó uno)

        assertEquals(
                403,
                post("/groups/" + locked + "/activate", team.member().token(), null)
                        .status());
        assertEquals(
                403,
                post("/groups/" + locked + "/activate", team.stranger().token(), null)
                        .status());
        assertEquals(404, post("/groups/999999/activate", leader.token(), null).status());
        assertEquals(401, post("/groups/" + locked + "/activate", null, null).status());
        assertTrue(locked(locked, leader));
    }
}
