package com.hub.synhub_backend.ai;

import com.hub.synhub_backend.ai.application.internal.queryservices.GroupReportQueryServiceImpl;
import com.hub.synhub_backend.ai.domain.model.queries.GenerateGroupReportQuery;
import com.hub.synhub_backend.metrics.domain.model.queries.GetRescheduledTasksInGroupQuery;
import com.hub.synhub_backend.metrics.domain.model.queries.GetTaskOverviewInGroupQuery;
import com.hub.synhub_backend.metrics.domain.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.domain.model.valueobjects.RescheduledTasks;
import com.hub.synhub_backend.metrics.domain.model.valueobjects.TaskOverview;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.GetAllTasksByGroupIdQuery;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Informe de grupo: datos enviados al modelo y manejo de fallos. */
class GroupReportQueryServiceImplTest {

    private static final long GROUP = 10;

    private BedrockRuntimeClient client;
    private TaskQueryService taskQueries;
    private GroupReportQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        client = mock(BedrockRuntimeClient.class);
        var metrics = mock(TaskMetricsQueryService.class);
        taskQueries = mock(TaskQueryService.class);
        service = new GroupReportQueryServiceImpl(client, metrics, taskQueries, "model-id");

        when(metrics.handle(new GetTaskOverviewInGroupQuery(GROUP)))
                .thenReturn(new TaskOverview("TASK_OVERVIEW", 3, Map.of("IN_PROGRESS", 2, "EXPIRED", 1)));
        when(metrics.handle(new GetRescheduledTasksInGroupQuery(GROUP)))
                .thenReturn(new RescheduledTasks("RESCHEDULED_TASKS", 2, Map.of("total", 3, "rescheduled", 2), List.of(2L)));

        var group = group(GROUP, user(1, "lider"), user(2, "ana"));
        var anaProfile = group.getUserProfiles().get(1);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        when(taskQueries.handle(new GetAllTasksByGroupIdQuery(GROUP))).thenReturn(List.of(
                task(anaProfile, TaskStatus.IN_PROGRESS, 5, now.plusHours(10), 2),   // vence en < 48 h
                task(anaProfile, TaskStatus.IN_PROGRESS, null, now.plusDays(10), 0), // sin dificultad → 3
                task(anaProfile, TaskStatus.EXPIRED, 2, now.minusDays(1), 0)));
    }

    private static Task task(com.hub.synhub_backend.groups.domain.model.entities.GroupProfile profile,
                             TaskStatus status, Integer difficulty, OffsetDateTime due, int rearranged) {
        var task = new Task();
        task.setProfile(profile);
        task.setStatus(status);
        ReflectionTestUtils.setField(task, "difficulty", difficulty);
        ReflectionTestUtils.setField(task, "dueDate", due);
        ReflectionTestUtils.setField(task, "timesRearranged", rearranged);
        return task;
    }

    private static ConverseResponse reply(String text) {
        return ConverseResponse.builder()
                .output(ConverseOutput.fromMessage(Message.builder().role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.fromText(text)).build()))
                .build();
    }

    @Test void sendsGroupFactsAndReturnsTheModelText() {
        when(client.converse(any(ConverseRequest.class))).thenReturn(reply("## Resumen\nTodo bien"));

        var report = service.handle(new GenerateGroupReportQuery(GROUP));

        assertEquals("## Resumen\nTodo bien", report.orElseThrow());
        var request = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(client).converse(request.capture());
        assertEquals("model-id", request.getValue().modelId());
        var facts = request.getValue().messages().getFirst().content().getFirst().text();
        assertTrue(facts.contains("Tareas por estado:"), facts);
        assertTrue(facts.contains("Reprogramadas (grupo):"), facts);
        // ana: 3 abiertas (EXPIRED cuenta como abierta) | carga 5+3+2=10 | 1 vencida | 2 reprogramaciones | 1 vence en 48h
        assertTrue(facts.contains("- ana Test: 3 | 10 | 1 | 2 | 1"), facts);
        assertTrue(facts.contains("Total de tareas: 3"), facts);
    }

    @Test void bedrockFailureReturnsEmpty() {
        when(client.converse(any(ConverseRequest.class))).thenThrow(BedrockRuntimeException.builder().message("throttled").build());
        assertTrue(service.handle(new GenerateGroupReportQuery(GROUP)).isEmpty());
    }
}
