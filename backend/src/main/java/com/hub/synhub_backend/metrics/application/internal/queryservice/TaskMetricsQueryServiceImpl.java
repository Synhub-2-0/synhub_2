package com.hub.synhub_backend.metrics.application.internal.queryservice;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.metrics.domain.exceptions.MetricNotAvailableException;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.model.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
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

    private final TaskRepository taskRepository;
    private final GroupQueryService groupQueryService;

    public TaskMetricsQueryServiceImpl(TaskRepository taskRepository, GroupQueryService groupQueryService) {
        this.taskRepository = taskRepository;
        this.groupQueryService = groupQueryService;
    }

    private List<Task> memberTasks(Long groupId, Long userId) {
        return taskRepository.findByProfile_Group_IdAndProfile_User_Id(groupId, userId);
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
    public TaskOverviewResource handle(GetTaskOverviewInGroupQuery query) {
        var tasks = taskRepository.findByProfile_Group_Id(query.groupId());
        return new TaskOverviewResource("TASK_OVERVIEW", tasks.size(), countByStatus(tasks));
    }

    @Override
    public TaskDistributionResource handle(GetTaskDistributionInGroupQuery query) {
        var tasks = taskRepository.findByProfile_Group_Id(query.groupId());
        Map<String, MemberTaskInfo> details = new HashMap<>();
        tasks.stream()
                .filter(task -> task.getProfile().getUser() != null)
                .collect(Collectors.groupingBy(task -> task.getProfile().getUser().getId()))
                .forEach((userId, memberTasks) ->
                        details.put(userId.toString(), new MemberTaskInfo(memberName(memberTasks.getFirst()), memberTasks.size())));
        return new TaskDistributionResource("TASK_DISTRIBUTION", tasks.size(), details);
    }

    @Override
    public RescheduledTasksResource handle(GetRescheduledTasksInGroupQuery query) {
        var tasks = taskRepository.findByProfile_Group_Id(query.groupId());
        var rescheduledUserIds = tasks.stream()
                .filter(task -> task.getTimesRearranged() > 0 && task.getProfile().getUser() != null)
                .map(task -> task.getProfile().getUser().getId())
                .distinct()
                .toList();
        var total = totalRescheduled(tasks);
        return new RescheduledTasksResource("RESCHEDULED_TASKS", total,
                Map.of("total", tasks.size(), "rescheduled", (int) total), rescheduledUserIds);
    }

    @Override
    public AvgCompletionTimeResource handle(GetAvgCompletionTimeInGroupQuery query) {
        if (groupQueryService.handle(new GetGroupByIdQuery(query.groupId())).isEmpty()) {
            return new AvgCompletionTimeResource("AVG_COMPLETION_TIME", 0, Map.of("completedTasks", 0));
        }
        var tasks = taskRepository.findByProfile_Group_Id(query.groupId());
        return new AvgCompletionTimeResource("AVG_COMPLETION_TIME", averageDaysToDone(tasks), Map.of("completedTasks", countDone(tasks)));
    }

    // ---- Integrante dentro del grupo (AUDIT BUG-08: solo las tareas de ese grupo)
    @Override
    public TaskOverviewResource handle(GetTaskOverviewForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.memberId());
        return new TaskOverviewResource("TASK_OVERVIEW_MEMBER", tasks.size(), countByStatus(tasks));
    }

    @Override
    public TaskDistributionResource handle(GetTaskDistributionForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        Map<String, MemberTaskInfo> details = tasks.isEmpty() ? Map.of()
                : Map.of(query.userId().toString(), new MemberTaskInfo(memberName(tasks.getFirst()), tasks.size()));
        return new TaskDistributionResource("TASK_DISTRIBUTION_MEMBER", tasks.size(), details);
    }

    @Override
    public RescheduledTasksResource handle(GetRescheduledTasksForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        var total = totalRescheduled(tasks);
        return new RescheduledTasksResource("RESCHEDULED_TASKS_MEMBER", total,
                Map.of("total", tasks.size(), "rescheduled", (int) total),
                total > 0 ? List.of(query.userId()) : List.of());
    }

    @Override
    public AvgCompletionTimeResource handle(GetAvgCompletionTimeForUserInGroupQuery query) {
        var tasks = memberTasks(query.groupId(), query.userId());
        return new AvgCompletionTimeResource("AVG_COMPLETION_TIME_MEMBER", averageDaysToDone(tasks), Map.of("completedTasks", countDone(tasks)));
    }

    @Override
    public TaskTimePassedResource handle(GetTaskTimePassedForUserInGroupQuery query) {
        var average = memberTasks(query.groupId(), query.userId()).stream()
                .mapToLong(Task::getTimePassed).average().orElse(0);
        return new TaskTimePassedResource(query.userId(), (long) average);
    }

    // ---- Una tarea
    private Task task(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " does not exist"));
    }

    @Override
    public TaskTimePassedResource handle(GetTaskTimePassedByIdQuery query) {
        var task = task(query.taskId());
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.DONE) {
            throw new MetricNotAvailableException("Time passed is only available for COMPLETED or DONE tasks");
        }
        var user = task.getProfile().getUser();
        return new TaskTimePassedResource(user != null ? user.getId() : null, task.getTimePassed());
    }

    @Override
    public TaskDurationResource handle(GetTaskDurationByIdQuery query) {
        var task = task(query.taskId());
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new MetricNotAvailableException("Duration is only available for IN_PROGRESS tasks");
        }
        // Plazo total de la tarea: de la creación a la fecha límite.
        long durationMillis = task.getDueDate().toInstant().toEpochMilli() - task.getCreatedAt().toInstant().toEpochMilli();
        return new TaskDurationResource(task.getId(), durationMillis / (1000 * 60 * 60));
    }
}
