package com.hub.synhub_backend.ai;

import com.hub.synhub_backend.ai.application.internal.queryservices.TaskClassificationQueryServiceImpl;
import com.hub.synhub_backend.ai.domain.model.queries.ClassifyTaskQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class TaskClassificationQueryServiceImplTest {

    private static final ClassifyTaskQuery QUERY = new ClassifyTaskQuery("Migrar BD", "Cambiar PK a uuid", null);

    private static ConverseResponse toolUse(Document input) {
        var block = ContentBlock.fromToolUse(ToolUseBlock.builder().toolUseId("1").name("classify_task").input(input).build());
        return ConverseResponse.builder()
                .output(ConverseOutput.fromMessage(Message.builder().role(ConversationRole.ASSISTANT).content(block).build()))
                .build();
    }

    private static Document input(String urgency, int difficulty, String context) {
        return Document.mapBuilder()
                .putDocument("labels", Document.fromList(List.of("a", "b", "c", "d").stream().map(Document::fromString).toList()))
                .putString("urgency", urgency)
                .putNumber("difficulty", difficulty)
                .putString("context", context)
                .putString("rationale", "porque sí")
                .build();
    }

    private static BedrockRuntimeClient clientReturning(ConverseResponse response) {
        var client = Mockito.mock(BedrockRuntimeClient.class);
        Mockito.when(client.converse(any(ConverseRequest.class))).thenReturn(response);
        return client;
    }

    @Test
    void mapsValidResponse() {
        var service = new TaskClassificationQueryServiceImpl(clientReturning(toolUse(input("HIGH", 4, "DATABASE"))), "m");
        var result = service.handle(QUERY).orElseThrow();
        assertEquals(4, result.difficulty());
        assertEquals("DATABASE", result.context());
        assertEquals("HIGH", result.urgency());
        assertEquals(3, result.labels().size()); // máximo 3 etiquetas
    }

    @Test
    void normalizesOutOfRangeValues() {
        var service = new TaskClassificationQueryServiceImpl(clientReturning(toolUse(input("URGENTISIMO", 9, "COCINA"))), "m");
        var result = service.handle(QUERY).orElseThrow();
        assertEquals(5, result.difficulty());
        assertEquals("GENERAL", result.context());
        assertEquals("MEDIUM", result.urgency());
    }

    @Test
    void returnsEmptyWhenBedrockFails() {
        var client = Mockito.mock(BedrockRuntimeClient.class);
        Mockito.when(client.converse(any(ConverseRequest.class))).thenThrow(new RuntimeException("sin acceso"));
        assertTrue(new TaskClassificationQueryServiceImpl(client, "m").handle(QUERY).isEmpty());
    }
}
