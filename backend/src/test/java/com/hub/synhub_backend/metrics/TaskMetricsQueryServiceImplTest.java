package com.hub.synhub_backend.metrics;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.metrics.application.internal.queryservices.TaskMetricsQueryServiceImpl;
import com.hub.synhub_backend.metrics.domain.exceptions.MetricNotAvailableException;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Cálculos de métricas sobre las tareas. */
class TaskMetricsQueryServiceImplTest {

    private static final long GROUP = 10, OTHER_GROUP = 11, ANA = 2, LUIS = 3;

    private TasksContextFacade tasks;
    private GroupQueryService groups;
    private TaskMetricsQueryServiceImpl service;
    private User ana, luis;
    private Group group, otherGroup;
    private GroupProfile anaProfile, luisProfile, anaOtherProfile;

    @BeforeEach
    void setUp() {
        tasks = mock(TasksContextFacade.class);
        groups = mock(GroupQueryService.class);
        service = new TaskMetricsQueryServiceImpl(tasks, groups);
        ana = user(ANA, "ana");
        luis = user(LUIS, "luis");
        group = group(GROUP, user(1, "lider"), ana, luis);
        otherGroup = group(OTHER_GROUP, user(5, "otro"), ana);
        anaProfile = group.getUserProfiles().get(1);
        luisProfile = group.getUserProfiles().get(2);
        anaOtherProfile = otherGroup.getUserProfiles().get(1);
        when(groups.handle(new GetGroupByIdQuery(GROUP))).thenReturn(Optional.of(group));
        when(groups.handle(new GetGroupByIdQuery(99L))).thenReturn(Optional.empty());
    }

    private static Task task(GroupProfile profile, TaskStatus status, int rearranged, Duration timePassed) {
        var task = new Task();
        task.setProfile(profile);
        task.setStatus(status);
        ReflectionTestUtils.setField(task, "timesRearranged", rearranged);
        ReflectionTestUtils.setField(task, "timePassed", timePassed.toMillis());
        return task;
    }

    @Test void overviewCountsTasksByStatus() {
        when(tasks.fetchTasksByGroupId(GROUP)).thenReturn(List.of(
                task(anaProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO),
                task(luisProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO),
                task(luisProfile, TaskStatus.DONE, 0, Duration.ofHours(4))));

        var overview = service.handle(new GetTaskOverviewInGroupQuery(GROUP));

        assertEquals("TASK_OVERVIEW", overview.type());
        assertEquals(3, overview.value());
        assertEquals(Map.of("IN_PROGRESS", 2, "DONE", 1), overview.details());
    }

    @Test void rescheduledSumsTimesAndListsMembers() {
        when(tasks.fetchTasksByGroupId(GROUP)).thenReturn(List.of(
                task(anaProfile, TaskStatus.IN_PROGRESS, 2, Duration.ZERO),
                task(anaProfile, TaskStatus.DONE, 1, Duration.ZERO),
                task(luisProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO)));

        var rescheduled = service.handle(new GetRescheduledTasksInGroupQuery(GROUP));

        assertEquals(3, rescheduled.value());
        assertEquals(Map.of("total", 3, "rescheduled", 3), rescheduled.details());
        assertEquals(List.of(ANA), rescheduled.rescheduledMemberIds());
    }

    @Test void distributionCountsTasksPerMember() {
        when(tasks.fetchTasksByGroupId(GROUP)).thenReturn(List.of(
                task(anaProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO),
                task(anaProfile, TaskStatus.DONE, 0, Duration.ZERO),
                task(luisProfile, TaskStatus.ON_HOLD, 0, Duration.ZERO)));

        var distribution = service.handle(new GetTaskDistributionInGroupQuery(GROUP));

        assertEquals(3, distribution.value());
        assertEquals(2, distribution.details().get(String.valueOf(ANA)).taskCount());
        assertEquals("ana Test", distribution.details().get(String.valueOf(ANA)).memberName());
        assertEquals(1, distribution.details().get(String.valueOf(LUIS)).taskCount());
    }

    @Test void averageCompletionOnlyCountsDoneTasksInDays() {
        when(tasks.fetchTasksByGroupId(GROUP)).thenReturn(List.of(
                task(anaProfile, TaskStatus.DONE, 0, Duration.ofDays(1)),
                task(luisProfile, TaskStatus.DONE, 0, Duration.ofDays(3)),
                task(luisProfile, TaskStatus.COMPLETED, 0, Duration.ofDays(30))));

        var average = service.handle(new GetAvgCompletionTimeInGroupQuery(GROUP));

        assertEquals(2.0, average.value(), 0.0001);
        assertEquals(Map.of("completedTasks", 2), average.details());
    }

    @Test void averageCompletionOfUnknownGroupIsZero() {
        var average = service.handle(new GetAvgCompletionTimeInGroupQuery(99L));
        assertEquals(0, average.value());
        assertEquals(Map.of("completedTasks", 0), average.details());
    }

    @Test void durationOnlyForTasksInProgress() {
        var inProgress = task(anaProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO);
        var created = OffsetDateTime.now(ZoneOffset.UTC);
        ReflectionTestUtils.setField(inProgress, "createdAt", created);
        ReflectionTestUtils.setField(inProgress, "dueDate", created.plusHours(48));
        ReflectionTestUtils.setField(inProgress, "id", 1L);
        when(tasks.getTaskById(1L)).thenReturn(Optional.of(inProgress));
        when(tasks.getTaskById(2L)).thenReturn(Optional.of(task(anaProfile, TaskStatus.DONE, 0, Duration.ZERO)));

        assertEquals(48L, service.handle(new GetTaskDurationByIdQuery(1L)).durationInHours());
        // 409 y 404 en lugar de 500 y 400
        assertThrows(MetricNotAvailableException.class, () -> service.handle(new GetTaskDurationByIdQuery(2L)));
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetTaskDurationByIdQuery(3L)));
    }

    @Test void timePassedOnlyForFinishedTasks() {
        when(tasks.getTaskById(1L)).thenReturn(Optional.of(task(anaProfile, TaskStatus.COMPLETED, 0, Duration.ofHours(5))));
        when(tasks.getTaskById(2L)).thenReturn(Optional.of(task(anaProfile, TaskStatus.IN_PROGRESS, 0, Duration.ZERO)));

        var result = service.handle(new GetTaskTimePassedByIdQuery(1L));
        assertEquals(ANA, result.memberId());
        assertEquals(Duration.ofHours(5).toMillis(), result.timePassed());
        assertThrows(MetricNotAvailableException.class, () -> service.handle(new GetTaskTimePassedByIdQuery(2L)));
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new GetTaskTimePassedByIdQuery(9L)));
    }

    // AUDIT BUG-08 (resuelto)
    @Test void perUserMetricsOnlyCountTheRequestedGroup() {
        var inGroup = task(anaProfile, TaskStatus.IN_PROGRESS, 1, Duration.ZERO);
        var inOtherGroup = task(anaOtherProfile, TaskStatus.IN_PROGRESS, 4, Duration.ZERO);
        when(tasks.fetchTasksByGroupIdAndUserId(GROUP, ANA)).thenReturn(List.of(inGroup));

        assertEquals(1, service.handle(new GetRescheduledTasksForUserInGroupQuery(ANA, GROUP)).value());
        assertEquals(1, service.handle(new GetTaskDistributionForUserInGroupQuery(ANA, GROUP)).value());
        assertEquals(1, service.handle(new GetTaskOverviewForUserInGroupQuery(ANA, GROUP)).value());
        assertEquals(0, service.handle(new GetAvgCompletionTimeForUserInGroupQuery(ANA, GROUP)).details().get("completedTasks"));
        service.handle(new GetTaskTimePassedForUserInGroupQuery(ANA, GROUP));
    }

    @Test void memberWithoutTasksHasEmptyMetrics() {
        when(tasks.fetchTasksByGroupIdAndUserId(GROUP, LUIS)).thenReturn(List.of());
        var distribution = service.handle(new GetTaskDistributionForUserInGroupQuery(LUIS, GROUP));
        assertEquals(0, distribution.value());
        assertTrue(distribution.details().isEmpty());
        assertEquals(0L, service.handle(new GetTaskTimePassedForUserInGroupQuery(LUIS, GROUP)).timePassed());
    }
}
