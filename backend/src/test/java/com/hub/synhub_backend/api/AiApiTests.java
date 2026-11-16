package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** IA por HTTP: lo que se resuelve antes de llamar al modelo (sesión, validación, permisos y grupo sin tareas). */
class AiApiTests extends ApiTestSupport {

    @Test
    void aiNeedsASession() {
        assertEquals(401, post("/ai/classify", null, Map.of("title", "Login")).status());
        assertEquals(401, get("/ai/groups/1/report", null).status());
    }

    @Test
    void classifyValidatesItsInput() {
        var account = account("ia");
        var noTitle = new HashMap<String, Object>();
        noTitle.put("description", "sin título");
        assertEquals(400, post("/ai/classify", account.token(), noTitle).status());
        assertEquals(
                400,
                post("/ai/classify", account.token(), Map.of("title", "   ")).status());
        assertEquals(
                400,
                post("/ai/classify", account.token(), Map.of("title", "a".repeat(256)))
                        .status());
        assertEquals(
                400,
                post("/ai/classify", account.token(), Map.of("title", "t", "description", "a".repeat(2001)))
                        .status());
        assertEquals(
                400,
                post("/ai/classify", account.token(), Map.of("title", "t", "dueDate", "mañana"))
                        .status());
    }

    @Test
    void theReportIsOnlyForTheLeaderOfAnExistingGroup() {
        var team = team();
        var path = "/ai/groups/" + team.groupId() + "/report";
        assertEquals(403, get(path, team.member().token()).status());
        assertEquals(403, get(path, team.stranger().token()).status());
        assertEquals(
                404, get("/ai/groups/99999999/report", team.leader().token()).status());
    }

    @Test
    void aGroupWithoutTasksGetsItsReportWithoutTheModel() {
        var team = team();
        var report =
                get("/ai/groups/" + team.groupId() + "/report", team.leader().token());
        assertEquals(200, report.status());
        assertTrue(report.text("report").contains("todavía no tiene tareas"), report.raw());
    }
}
