package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La clasificación de una tarea es opcional al crearla y se puede editar, rehacer o quitar después. */
class TaskClassificationApiTests extends ApiTestSupport {

    private Map<String, Object> update(Team team, Map<String, Object> classification) {
        var body = new HashMap<String, Object>(Map.of("userId", team.member().id()));
        if (classification != null) body.put("classification", classification);
        return body;
    }

    private static Map<String, Object> classification(
            Integer difficulty, String context, String urgency, String labels, Boolean aiAccepted) {
        var map = new HashMap<String, Object>();
        map.put("difficulty", difficulty);
        map.put("context", context);
        map.put("urgency", urgency);
        map.put("labels", labels);
        map.put("aiAccepted", aiAccepted);
        return map;
    }

    @Test
    void aTaskCanBeCreatedWithoutClassificationAndClassifiedLater() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var created = get("/tasks/" + taskId, team.leader().token());
        assertTrue(created.json().get("difficulty").isNull());
        assertTrue(created.json().get("aiAccepted").isNull());

        var classified = put(
                "/tasks/" + taskId,
                team.leader().token(),
                update(team, classification(4, "BACKEND", "HIGH", "api, auth", true)));
        assertEquals(200, classified.status());
        assertEquals(4, classified.json().get("difficulty").asInt());
        assertEquals("BACKEND", classified.text("context"));
        assertEquals("HIGH", classified.text("urgency"));
        assertEquals("api, auth", classified.text("labels"));
        assertTrue(classified.json().get("aiAccepted").asBoolean());
    }

    @Test
    void theClassificationCanBeEditedKeptOrRemoved() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var path = "/tasks/" + taskId;
        put(path, team.leader().token(), update(team, classification(4, "BACKEND", "HIGH", "api", true)));

        var edited = put(path, team.leader().token(), update(team, classification(2, "QA", "HIGH", "api", false)));
        assertEquals(2, edited.json().get("difficulty").asInt());
        assertEquals("QA", edited.text("context"));
        assertFalse(edited.json().get("aiAccepted").asBoolean());

        var untouched = put(path, team.leader().token(), update(team, null));
        assertEquals(2, untouched.json().get("difficulty").asInt(), "sin classification no cambia");
        assertEquals("QA", untouched.text("context"));

        var removed = put(path, team.leader().token(), update(team, classification(null, null, null, null, true)));
        assertEquals(200, removed.status());
        var saved = get(path, team.leader().token()).json();
        assertTrue(saved.get("difficulty").isNull());
        assertTrue(saved.get("context").isNull());
        assertTrue(saved.get("urgency").isNull());
        assertTrue(saved.get("labels").isNull());
        assertTrue(saved.get("aiAccepted").isNull(), "sin clasificación no hay medición de la IA");
    }

    @Test
    void theAiRationaleIsSavedWithTheTaskAndRemovedWithTheClassification() {
        var team = team();
        var body = taskBody(team.groupId(), team.member().id(), unique("Tarea"));
        body.putAll(Map.of("difficulty", 4, "context", "BACKEND", "aiAccepted", true, "aiRationale", "Toca el token."));
        var created = post("/tasks", team.leader().token(), body);
        assertEquals(200, created.status());
        var path = "/tasks/" + created.id();
        assertEquals("Toca el token.", get(path, team.member().token()).text("aiRationale"));

        var redone = classification(2, "QA", null, null, true);
        redone.put("aiRationale", "Solo pruebas.");
        assertEquals(
                "Solo pruebas.",
                put(path, team.leader().token(), update(team, redone)).text("aiRationale"));

        put(path, team.leader().token(), update(team, classification(null, null, null, null, null)));
        assertTrue(get(path, team.leader().token()).json().get("aiRationale").isNull());

        var tooLong = classification(2, "QA", null, null, true);
        tooLong.put("aiRationale", "a".repeat(501));
        assertEquals(
                400, put(path, team.leader().token(), update(team, tooLong)).status());
    }

    @Test
    void anInvalidClassificationIs400AndOnlyTheLeaderEdits() {
        var team = team();
        var taskId = createTask(team.leader(), team.groupId(), team.member());
        var path = "/tasks/" + taskId;
        assertEquals(
                400,
                put(path, team.leader().token(), update(team, classification(9, "QA", "LOW", null, null)))
                        .status());
        assertEquals(
                403,
                put(path, team.member().token(), update(team, classification(3, "QA", "LOW", null, null)))
                        .status());
        assertTrue(get(path, team.leader().token()).json().get("difficulty").isNull());
    }
}
