package com.hub.synhub_backend.ai.application.internal.queryservices;

import com.hub.synhub_backend.ai.domain.model.queries.GenerateGroupReportQuery;
import com.hub.synhub_backend.ai.domain.services.GroupReportQueryService;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupMembersByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.services.TaskMetricsQueryService;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByGroupIdQuery;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

@Service
public class GroupReportQueryServiceImpl implements GroupReportQueryService {

    private static final Logger log = LoggerFactory.getLogger(GroupReportQueryServiceImpl.class);
    private static final int DEFAULT_DIFFICULTY = 3;

    private static final String SYSTEM = """
            Eres el analista de productividad de Synhub. Recibes métricas reales de un grupo de trabajo y \
            escribes un informe breve en español para el líder, en markdown, con estas secciones: \
            ## Resumen (2 frases), ## Carga del equipo, ## Riesgos, ## Recomendaciones (máx. 3 acciones concretas). \
            Usa solo los datos dados, cita nombres y números, no inventes datos. La "carga ponderada" suma la \
            dificultad (1-5) de las tareas abiertas: es mejor indicador que contar tareas. Si hay pocos datos, dilo.
            Empieza directamente en ## Resumen: sin título general y sin fecha. No uses tablas. \
            La lista incluye a todos los integrantes del grupo: quien aparece con 0 tareas abiertas está disponible \
            para recibir trabajo. No pidas más datos ni sugieras revisar el sistema: recomienda solo acciones \
            sobre las tareas y las personas del grupo.""";

    /** Un grupo sin tareas no tiene nada que analizar: se responde sin llamar al modelo. */
    static final String EMPTY_GROUP_REPORT = """
            ## Resumen
            Este grupo todavía no tiene tareas, así que aún no hay nada que analizar.
            ## Recomendaciones
            - Crea y asigna las primeras tareas: el informe usará su estado, dificultad y fecha de entrega.""";

    private final BedrockRuntimeClient client;
    private final TaskMetricsQueryService metrics;
    private final TaskQueryService taskQueryService;
    private final GroupQueryService groupQueryService;
    private final String modelId;

    public GroupReportQueryServiceImpl(
            BedrockRuntimeClient client,
            TaskMetricsQueryService metrics,
            TaskQueryService taskQueryService,
            GroupQueryService groupQueryService,
            @Value("${ai.bedrock.model-id}") String modelId) {
        this.client = client;
        this.metrics = metrics;
        this.taskQueryService = taskQueryService;
        this.groupQueryService = groupQueryService;
        this.modelId = modelId;
    }

    @Override
    public Optional<String> handle(GenerateGroupReportQuery query) {
        var tasks = taskQueryService.handle(new GetAllTasksByGroupIdQuery(query.groupId()));
        if (tasks.isEmpty()) return Optional.of(EMPTY_GROUP_REPORT);
        try {
            var response = client.converse(ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(SYSTEM).build())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(buildFacts(query, tasks)))
                            .build())
                    .inferenceConfig(c -> c.maxTokens(900).temperature(0.2f))
                    .build());
            return Optional.of(response.output().message().content().get(0).text());
        } catch (RuntimeException e) {
            log.warn("Fallo el informe con Bedrock: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildFacts(GenerateGroupReportQuery query, List<Task> tasks) {
        var groupId = query.groupId();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var overview = metrics.handle(new GetTaskOverviewInGroupQuery(groupId));
        var rescheduled = metrics.handle(new GetRescheduledTasksInGroupQuery(groupId));

        // [abiertas, carga ponderada, vencidas, reprogramadas, vencen en 48h, abiertas de urgencia alta]
        Map<String, int[]> perMember = new TreeMap<>();
        // Todos los integrantes, también los que no tienen tareas: sin ellos el modelo no sabe a quién reasignar.
        for (var member : groupQueryService.handle(new GetGroupMembersByGroupIdQuery(groupId, query.requesterId()))) {
            perMember.put(fullName(member.getUser()), new int[6]);
        }
        for (Task t : tasks) {
            var row = perMember.computeIfAbsent(fullName(t.getProfile().getUser()), k -> new int[6]);
            boolean open = t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.DONE;
            if (open) {
                row[0]++;
                row[1] += t.getDifficulty() != null ? t.getDifficulty() : DEFAULT_DIFFICULTY;
                if (t.getDueDate().isBefore(now.plusHours(48)) && t.getStatus() != TaskStatus.EXPIRED) row[4]++;
                if ("HIGH".equals(t.getUrgency())) row[5]++;
            }
            if (t.getStatus() == TaskStatus.EXPIRED) row[2]++;
            row[3] += t.getTimesRearranged();
        }

        var sb = new StringBuilder("Fecha actual: " + now + "\n");
        sb.append("Tareas por estado: ").append(overview.details()).append("\n");
        sb.append("Reprogramadas (grupo): ").append(rescheduled.details()).append("\n");
        sb.append("Por integrante (abiertas | carga ponderada | vencidas | reprogramaciones | vencen en 48h"
                + " | abiertas de urgencia alta):\n");
        perMember.forEach((n, r) ->
                sb.append("- %s: %d | %d | %d | %d | %d | %d\n".formatted(n, r[0], r[1], r[2], r[3], r[4], r[5])));
        sb.append("Total de tareas: ").append(tasks.size());
        return sb.toString();
    }

    private static String fullName(User user) {
        return user.getName() + " " + user.getSurname();
    }
}
