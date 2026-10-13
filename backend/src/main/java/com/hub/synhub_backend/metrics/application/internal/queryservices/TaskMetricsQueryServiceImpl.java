package com.hub.synhub_backend.metrics.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.metrics.domain.exceptions.MetricNotAvailableException;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.domain.model.valueobjects.*;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Métricas de tareas de un grupo o de un integrante dentro de un grupo.
 * Las consultas "ForUserInGroup" solo cuentan las tareas del grupo pedido (AUDIT BUG-08).
 * Las tareas borradas (soft delete, HUM-03) no cuentan: el repositorio ya las excluye.
 */
@Service
@Transactional(readOnly = true)
public class TaskMetricsQueryServiceImpl implements TaskMetricsQueryService {

    private static final double MILLIS_PER_DAY = 1000d * 60 * 60 * 24;

    private final TasksContextFacade tasksContextFacade;
    private final GroupQueryService groupQueryService;

    public TaskMetricsQueryServiceImpl(TasksContextFacade tasksContextFacade, GroupQueryService groupQueryService) {
        this.tasksContextFacade = tasksContextFacade;
        this.groupQueryService = groupQueryService;
    }

    private List<Task> memberTasks(Long groupId, Long userId) {
        return tasksContextFacade.fetchTasksByGroupIdAndUserId(groupId, userId);
    }

    private static Map<String, Integer> countByStatus(List<Task> tasks) {
        return tasks.stream().collect(Collectors.groupingBy(task -> task.getStatus().name(), Collectors.summingInt(task -> 1)));
    }

    private static String memberName(Task task) {
        var user = task.getProfile().getUser();
        return user.getName() + " " + user.getSurname();
    }

    private static double averageDaysToDone(List<Task> tasks) {
        return tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE)
                .mapToLong(Task::getTimePassed).average().orElse(0) / MILLIS_PER_DAY;
    }

    private static int countDone(List<Task> tasks) {
        return (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
    }

    private static long totalRescheduled(List<Task> tasks) {
        return tasks.stream().mapToLong(Task::getTimesRearranged).sum();
    }

    // ---- Grupo
    @Override
    public TaskOverview handle(GetTaskOverviewInGroupQuery query) {
        var tasks = tasksContextFacade.fetchTasksByGroupId(query.groupId());
        return new TaskOverview("TASK_OVERVIEW", tasks.size(), countByStatus(tasks));
    }

    @Override
    public TaskDistribution handle(GetTaskDistributionInGroupQuery query) {
        var tasks = tasksContextFacade.fetchTasksByGroupId(query.groupId());
        Map<String, MemberTaskCount> details = new HashMap<>();
        tasks.stream()
                .filter(task -> task.getProfile().getUser() != null)
                .collect(Collectors.groupingBy(task -> task.getProfile().getUser().getId()))
                .forEach((userId, memberTasks) ->
                        details.put(userId.toString(), new MemberTaskCount(memberName(memberTasks.getFirst()), memberTasks.size())));
        return new TaskDistribution("TASK_DISTRIBUTION", tasks.size(), details);
    }

    @Override
    public RescheduledTasks handle(GetRescheduledTasksInGroupQuery query) {
        var tasks = tasksContextFacade.fetchTasksByGroupId(query.groupId());
        var rescheduledUserIds = tasks.stream()
                .filter(task -> task.getTimesRearranged() > 0 && task.getProfile().getUser() != null)
                .map(task -> task.getProfile().getUser().getId())
                .distinct()
                .toList();
        var total = totalRescheduled(tasks);
        return new RescheduledTasks("RESCHEDULED_TASKS", total,
                Map.of("total", tasks.size(), "rescheduled", (int) total), rescheduledUserIds);
    }

    @Override
    public AvgCompletionTime handle(GetAvgCompletionTimeInGroupQuery query) {
        if (groupQueryService.handle(new GetGroupByIdQuery(query.groupId())).isEmpty()) {
            return new AvgCompletionTime("AVG_COMPLETION_TIME", 0, Map.of("completedTasks", 0));
        }
        var tasks = tasksContextFacade.fetchTasksByGroupId(query.groupId());
        return new AvgCompletionTime("AVG_COMPLETION_TIME", averageDaysToDone(tasks), Map.of("completedTasks", countDone(tasks)));
    }

    // ---- Integrante dentro del grupo (AUDIT BUG-08: solo las tareas de ese grupo)
    @Override
    public TaskOverview handle(GetTaskOverviewForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.memberId());
        return new TaskOverview("TASK_OVERVIEW_MEMBER", tasks.size(), countByStatus(tasks));
    }

    @Override
    public TaskDistribution handle(GetTaskDistributionForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        Map<String, MemberTaskCount> details = tasks.isEmpty() ? Map.of()
                : Map.of(query.userId().toString(), new MemberTaskCount(memberName(tasks.getFirst()), tasks.size()));
        return new TaskDistribution("TASK_DISTRIBUTION_MEMBER", tasks.size(), details);
    }

    @Override
    public RescheduledTasks handle(GetRescheduledTasksForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        var total = totalRescheduled(tasks);
        return new RescheduledTasks("RESCHEDULED_TASKS_MEMBER", total,
                Map.of("total", tasks.size(), "rescheduled", (int) total),
                total > 0 ? List.of(query.userId()) : List.of());
    }

    @Override
    public AvgCompletionTime handle(GetAvgCompletionTimeForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        return new AvgCompletionTime("AVG_COMPLETION_TIME_MEMBER", averageDaysToDone(tasks), Map.of("completedTasks", countDone(tasks)));
    }

    @Override
    public TaskTimePassed handle(GetTaskTimePassedForUserInGroupQuery query) {
        var average = memberTasks(query.groupId(), query.userId()).stream()
                .mapToLong(Task::getTimePassed).average().orElse(0);
        return new TaskTimePassed(query.userId(), (long) average);
    }

    // ---- Una tarea
    private Task task(Long taskId) {
        return tasksContextFacade.getTaskById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
    }

    @Override
    public TaskTimePassed handle(GetTaskTimePassedByIdQuery query) {
        var task = task(query.taskId());
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.DONE) {
            throw new MetricNotAvailableException("Time passed is only available for COMPLETED or DONE tasks");
        }
        var user = task.getProfile().getUser();
        return new TaskTimePassed(user != null ? user.getId() : null, task.getTimePassed());
    }

    @Override
    public TaskDuration handle(GetTaskDurationByIdQuery query) {
        var task = task(query.taskId());
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new MetricNotAvailableException("Duration is only available for IN_PROGRESS tasks");
        }
        // Plazo total de la tarea: de la creación a la fecha límite.
        long durationMillis = task.getDueDate().toInstant().toEpochMilli() - task.getCreatedAt().toInstant().toEpochMilli();
        return new TaskDuration(task.getId(), durationMillis / (1000 * 60 * 60));
    }
}
