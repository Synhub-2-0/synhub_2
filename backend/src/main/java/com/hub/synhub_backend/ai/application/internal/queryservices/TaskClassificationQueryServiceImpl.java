package com.hub.synhub_backend.ai.application.internal.queryservices;

import com.hub.synhub_backend.ai.domain.model.queries.ClassifyTaskQuery;
import com.hub.synhub_backend.ai.domain.model.valueobjects.TaskClassification;
import com.hub.synhub_backend.ai.domain.services.TaskClassificationQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskClassificationQueryServiceImpl implements TaskClassificationQueryService {

    private static final Logger log = LoggerFactory.getLogger(TaskClassificationQueryServiceImpl.class);
    private static final String TOOL = "classify_task";
    private static final List<String> CONTEXTS =
            List.of("FRONTEND", "BACKEND", "DATABASE", "CLOUD", "QA", "DOCS", "DESIGN", "GENERAL");
    private static final List<String> URGENCIES = List.of("LOW", "MEDIUM", "HIGH");

    private static final String SYSTEM = """
            Eres el asistente de Synhub, una app de gestión ágil de tareas para equipos. \
            Clasifica la tarea con la herramienta classify_task. Responde en español.
            - labels: 1 a 3 etiquetas cortas (1-2 palabras) derivadas del texto.
            - urgency: LOW, MEDIUM o HIGH. Considera palabras como "urgente", "bloquea" y la fecha límite respecto a la fecha actual.
            - difficulty: 1 (trivial, minutos) a 5 (muy compleja, varios días o incertidumbre alta).
            - context: el área técnica principal.
            - rationale: una sola frase (máx. 25 palabras) que justifique la dificultad y el contexto.
            Usa solo el texto dado, no inventes requisitos.""";

    private final BedrockRuntimeClient client;
    private final String modelId;

    public TaskClassificationQueryServiceImpl(BedrockRuntimeClient client,
                                              @Value("${ai.bedrock.model-id}") String modelId) {
        this.client = client;
        this.modelId = modelId;
    }

    @Override
    public Optional<TaskClassification> handle(ClassifyTaskQuery query) {
        var userText = "Fecha actual: %s\nFecha límite: %s\nTítulo: %s\nDescripción: %s".formatted(
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), query.dueDate(), query.title(), query.description());
        try {
            var response = client.converse(ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(SYSTEM).build())
                    .messages(Message.builder().role(ConversationRole.USER)
                            .content(ContentBlock.fromText(userText)).build())
                    .toolConfig(ToolConfiguration.builder()
                            .tools(Tool.builder().toolSpec(toolSpec()).build())
                            .toolChoice(ToolChoice.builder().tool(SpecificToolChoice.builder().name(TOOL).build()).build())
                            .build())
                    .inferenceConfig(c -> c.maxTokens(400).temperature(0f))
                    .build());
            return response.output().message().content().stream()
                    .filter(b -> b.toolUse() != null)
                    .findFirst()
                    .map(b -> toClassification(b.toolUse().input().asMap()));
        } catch (RuntimeException e) {
            log.warn("Fallo la clasificación con Bedrock: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static TaskClassification toClassification(Map<String, Document> in) {
        var labels = in.get("labels").asList().stream().map(Document::asString).limit(3).toList();
        var urgency = in.get("urgency").asString();
        var context = in.get("context").asString();
        return new TaskClassification(
                labels,
                URGENCIES.contains(urgency) ? urgency : "MEDIUM",
                Math.max(1, Math.min(5, in.get("difficulty").asNumber().intValue())),
                CONTEXTS.contains(context) ? context : "GENERAL",
                in.get("rationale").asString());
    }

    private static ToolSpecification toolSpec() {
        var schema = Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("labels", Document.mapBuilder().putString("type", "array")
                                .putDocument("items", Document.mapBuilder().putString("type", "string").build()).build())
                        .putDocument("urgency", enumOf(URGENCIES))
                        .putDocument("difficulty", Document.mapBuilder().putString("type", "integer").build())
                        .putDocument("context", enumOf(CONTEXTS))
                        .putDocument("rationale", Document.mapBuilder().putString("type", "string").build())
                        .build())
                .putDocument("required", Document.fromList(
                        List.of("labels", "urgency", "difficulty", "context", "rationale").stream().map(Document::fromString).toList()))
                .build();
        return ToolSpecification.builder().name(TOOL)
                .description("Registra la clasificación de una tarea")
                .inputSchema(ToolInputSchema.builder().json(schema).build())
                .build();
    }

    private static Document enumOf(List<String> values) {
        return Document.mapBuilder().putString("type", "string")
                .putDocument("enum", Document.fromList(values.stream().map(Document::fromString).toList())).build();
    }
}
