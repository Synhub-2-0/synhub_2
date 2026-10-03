package com.hub.synhub_backend.ai.application.internal.queryservices;

import com.hub.synhub_backend.ai.domain.model.queries.GenerateGroupReportQuery;
import com.hub.synhub_backend.ai.domain.services.GroupReportQueryService;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.model.services.TaskMetricsQueryService;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByGroupIdQuery;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class GroupReportQueryServiceImpl implements GroupReportQueryService {

    private static final Logger log = LoggerFactory.getLogger(GroupReportQueryServiceImpl.class);
    private static final int DEFAULT_DIFFICULTY = 3;

    private static final String SYSTEM = """
            Eres el analista de productividad de Synhub. Recibes métricas reales de un grupo de trabajo y \
            escribes un informe breve en español para el líder, en markdown, con estas secciones: \
            ## Resumen (2 frases), ## Carga del equipo, ## Riesgos, ## Recomendaciones (máx. 3 acciones concretas). \
            Usa solo los datos dados, cita nombres y números, no inventes datos. La "carga ponderada" suma la \
            dificultad (1-5) de las tareas abiertas: es mejor indicador que contar tareas. Si hay pocos datos, dilo.""";

    private final BedrockRuntimeClient client;
    private final TaskMetricsQueryService metrics;
    private final TaskQueryService taskQueryService;
    private final String modelId;

    public GroupReportQueryServiceImpl(BedrockRuntimeClient client, TaskMetricsQueryService metrics,
                                       TaskQueryService taskQueryService, @Value("${ai.bedrock.model-id}") String modelId) {
        this.client = client;
        this.metrics = metrics;
        this.taskQueryService = taskQueryService;
        this.modelId = modelId;
    }

    @Override
    public Optional<String> handle(GenerateGroupReportQuery query) {
        try {
            var response = client.converse(ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(SYSTEM).build())
                    .messages(Message.builder().role(ConversationRole.USER)
                            .content(ContentBlock.fromText(buildFacts(query.groupId()))).build())
                    .inferenceConfig(c -> c.maxTokens(900).temperature(0.2f))
                    .build());
            return Optional.of(response.output().message().content().get(0).text());
        } catch (RuntimeException e) {
            log.warn("Fallo el informe con Bedrock: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildFacts(Long groupId) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var overview = metrics.handle(new GetTaskOverviewInGroupQuery(groupId));
        var rescheduled = metrics.handle(new GetRescheduledTasksInGroupQuery(groupId));
        var tasks = taskQueryService.handle(new GetAllTasksByGroupIdQuery(groupId));

        Map<String, int[]> perMember = new TreeMap<>(); // [abiertas, carga ponderada, vencidas, reprogramadas, vencen en 48h]
        for (Task t : tasks) {
            var user = t.getProfile().getUser();
            var name = user.getName() + " " + user.getSurname();
            var row = perMember.computeIfAbsent(name, k -> new int[5]);
            boolean open = t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.DONE;
            if (open) {
                row[0]++;
                row[1] += t.getDifficulty() != null ? t.getDifficulty() : DEFAULT_DIFFICULTY;
                if (t.getDueDate().isBefore(now.plusHours(48)) && t.getStatus() != TaskStatus.EXPIRED) row[4]++;
            }
            if (t.getStatus() == TaskStatus.EXPIRED) row[2]++;
            row[3] += t.getTimesRearranged();
        }

        var sb = new StringBuilder("Fecha actual: " + now + "\n");
        sb.append("Tareas por estado: ").append(overview.details()).append("\n");
        sb.append("Reprogramadas (grupo): ").append(rescheduled.details()).append("\n");
        sb.append("Por miembro (abiertas | carga ponderada | vencidas | reprogramaciones | vencen en 48h):\n");
        perMember.forEach((n, r) -> sb.append("- %s: %d | %d | %d | %d | %d\n".formatted(n, r[0], r[1], r[2], r[3], r[4])));
        sb.append("Total de tareas: ").append(tasks.size());
        return sb.toString();
    }
}
