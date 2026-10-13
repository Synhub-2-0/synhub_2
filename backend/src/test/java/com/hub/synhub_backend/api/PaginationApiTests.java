package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** AUDIT PERF-02: paginación opcional; sin parámetros la respuesta sigue siendo un arreglo. */
class PaginationApiTests extends ApiTestSupport {

    @Test
    void groupTasksArePagedOnlyWhenAsked() {
        var team = team();
        for (int i = 0; i < 3; i++) createTask(team.leader(), team.groupId(), team.member());
        var path = "/tasks/group/" + team.groupId();

        var all = get(path, team.leader().token());
        assertTrue(all.json().isArray(), "sin parámetros: arreglo, como antes");
        assertEquals(3, all.json().size());

        var first = get(path + "?size=1", team.leader().token());
        assertEquals(200, first.status(), first.raw());
        assertEquals(1, first.json().get("content").size());
        assertEquals(0, first.json().get("page").asInt());
        assertEquals(1, first.json().get("size").asInt());
        assertEquals(3, first.json().get("totalElements").asLong());
        assertEquals(3, first.json().get("totalPages").asInt());

        var last = get(path + "?page=1&size=2", team.leader().token());
        assertEquals(1, last.json().get("content").size());

        assertEquals(
                0,
                get(path + "?page=5&size=2", team.leader().token())
                        .json()
                        .get("content")
                        .size());
        assertEquals(
                1,
                get(path + "?status=IN_PROGRESS&page=0&size=1", team.leader().token())
                        .json()
                        .get("content")
                        .size());
    }

    @Test
    void groupRequestsArePagedNewestFirst() {
        var team = team();
        long newest = 0;
        for (int i = 0; i < 3; i++)
            newest = submit(team.member(), createTask(team.leader(), team.groupId(), team.member()));
        var path = "/groups/" + team.groupId() + "/requests";

        assertTrue(get(path, team.leader().token()).json().isArray());
        var page = get(path + "?type=SUBMISSION&page=0&size=2", team.leader().token());
        assertEquals(200, page.status(), page.raw());
        assertEquals(2, page.json().get("content").size());
        assertEquals(newest, page.json().get("content").get(0).get("id").asLong(), "la más nueva primero");
        assertEquals(3, page.json().get("totalElements").asLong());
        assertEquals(403, get(path + "?size=1", team.member().token()).status(), "sigue siendo solo para el líder");
    }

    @Test
    void invalidOrOversizedPages() {
        var team = team();
        var path = "/tasks/group/" + team.groupId();
        assertEquals(400, get(path + "?page=-1", team.leader().token()).status());
        assertEquals(400, get(path + "?size=0", team.leader().token()).status());
        assertEquals(
                100,
                get(path + "?size=500", team.leader().token())
                        .json()
                        .get("size")
                        .asInt(),
                "size se limita a 100");
    }

    // AUDIT PERF-03: mis tareas de todos mis grupos en una sola petición
    @Test
    void myTasksAcrossGroupsInOneRequest() {
        var team = team();
        var otherLeader = account("otro");
        var otherGroup = createGroup(otherLeader);
        join(otherLeader, team.member(), otherGroup);
        var first = createTask(team.leader(), team.groupId(), team.member());
        var second = createTask(otherLeader, otherGroup, team.member());
        createTask(team.leader(), team.groupId(), team.leader());

        var mine = get("/tasks/user", team.member().token());
        assertEquals(200, mine.status(), mine.raw());
        assertEquals(2, mine.json().size(), "solo las asignadas al usuario, de ambos grupos");
        var ids = new java.util.HashSet<Long>();
        for (var task : mine.json()) ids.add(task.get("id").asLong());
        assertEquals(java.util.Set.of(first, second), ids);
        assertEquals(0, get("/tasks/user", team.stranger().token()).json().size());
    }
}
