package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Endpoints base de métricas (decisión HUM-05) y su control de acceso. */
class MetricsApiTests extends ApiTestSupport {

    @Test void groupMetricsForMembers() {
        var team = team();
        createTask(team.leader(), team.groupId(), team.member());
        var done = createTask(team.leader(), team.groupId(), team.member());
        submit(team.member(), done);

        var overview = get("/metrics/groups/" + team.groupId() + "/overview", team.member().token());
        assertEquals(200, overview.status(), overview.raw());
        assertEquals(2, overview.json().get("value").asInt());
        assertEquals(1, overview.json().get("details").get("COMPLETED").asInt());

        var distribution = get("/metrics/groups/" + team.groupId() + "/distribution", team.leader().token());
        assertEquals(2, distribution.json().get("details").get(String.valueOf(team.member().id())).get("taskCount").asInt());
        assertEquals(200, get("/metrics/groups/" + team.groupId() + "/rescheduled", team.leader().token()).status());
        assertEquals(200, get("/metrics/groups/" + team.groupId() + "/avg-completion-time", team.leader().token()).status());

        assertEquals(403, get("/metrics/groups/" + team.groupId() + "/overview", team.stranger().token()).status());
        assertEquals(404, get("/metrics/groups/999999/overview", team.leader().token()).status());
    }

    @Test void memberMetricsForTheLeaderOrTheMember() {
        var team = team();
        var other = account("otro");
        join(team.leader(), other, team.groupId());
        createTask(team.leader(), team.groupId(), team.member());
        var path = "/metrics/groups/" + team.groupId() + "/members/" + team.member().id();

        assertEquals(1, get(path + "/overview", team.leader().token()).json().get("value").asInt());
        assertEquals(200, get(path + "/distribution", team.member().token()).status());
        assertEquals(200, get(path + "/rescheduled", team.member().token()).status());
        assertEquals(200, get(path + "/avg-completion-time", team.member().token()).status());
        assertEquals(200, get(path + "/time-passed", team.member().token()).status());

        assertEquals(403, get(path + "/overview", other.token()).status(), "otro miembro no ve las métricas ajenas");
        assertEquals(403, get(path + "/overview", team.stranger().token()).status());
        assertEquals(404, get("/metrics/groups/" + team.groupId() + "/members/" + team.stranger().id() + "/overview", team.leader().token()).status());
    }

    @Test void taskMetrics() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var duration = get("/metrics/tasks/" + taskId + "/duration", team.member().token());
        assertEquals(200, duration.status(), duration.raw());
        assertEquals(72, duration.json().get("durationInHours").asLong(), 1);
        assertEquals(409, get("/metrics/tasks/" + taskId + "/time-passed", team.member().token()).status(), "la tarea no terminó");

        submit(team.member(), taskId);
        assertEquals(200, get("/metrics/tasks/" + taskId + "/time-passed", team.leader().token()).status());
        assertEquals(409, get("/metrics/tasks/" + taskId + "/duration", team.leader().token()).status());

        assertEquals(403, get("/metrics/tasks/" + taskId + "/duration", team.stranger().token()).status());
        assertEquals(404, get("/metrics/tasks/999999/duration", team.leader().token()).status());
    }
}
