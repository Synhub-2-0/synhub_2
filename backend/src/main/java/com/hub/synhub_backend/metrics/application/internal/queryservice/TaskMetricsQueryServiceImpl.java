package com.hub.synhub_backend.metrics.application.internal.queryservice;

import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.model.services.TaskMetricsQueryService;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskMetricsQueryServiceImpl implements TaskMetricsQueryService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GroupQueryService groupQueryService;

    public TaskMetricsQueryServiceImpl(TaskRepository taskRepository, UserRepository userRepository, GroupQueryService groupQueryService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.groupQueryService = groupQueryService;
    }

    @Override
    public TaskTimePassedResource handle(GetTaskTimePassedForUserInGroupQuery query) {
        List<Task> memberTasks = taskRepository.findByProfile_User_Id(query.userId());

        double avgTimePassed = memberTasks.isEmpty() ? 0 :
            memberTasks.stream().mapToLong(Task::getTimePassed).average().orElse(0);

        return new TaskTimePassedResource(query.userId(), (long) avgTimePassed);
    }

    @Override
    public AvgCompletionTimeResource handle(GetAvgCompletionTimeInGroupQuery query) {
        var groupOpt = groupQueryService.handle(new GetGroupByIdQuery(query.groupId()));
        if (groupOpt.isEmpty()) {
            return new AvgCompletionTimeResource(
                    "AVG_COMPLETION_TIME",
                    0,
                    Map.of("completedTasks", 0)
            );
        }
        Long groupId = groupOpt.get().getId();

        List<Task> groupTasks = taskRepository.findByProfile_Group_Id(groupId);

        List<Task> completedTasks = groupTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .collect(Collectors.toList());

        double avg = completedTasks.stream()
                .mapToLong(Task::getTimePassed)
                .average()
                .orElse(0);

        return new AvgCompletionTimeResource(
                "AVG_COMPLETION_TIME",
                avg / (1000 * 60 * 60 * 24),
                Map.of("completedTasks", completedTasks.size())
        );
    }

    @Override
    public RescheduledTasksResource handle(GetRescheduledTasksInGroupQuery query) {
        List<Task> groupTasks = taskRepository.findByProfile_Group_Id(query.groupId());

        long totalRescheduledTimes = groupTasks.stream()
                .mapToLong(Task::getTimesRearranged)
                .sum();

        Map<String, Integer> details = Map.of(
                "total", groupTasks.size(),
                "rescheduled", (int) totalRescheduledTimes
        );

        List<Long> rescheduledUserIds = groupTasks.stream()
                .filter(task -> task.getTimesRearranged() > 0 && task.getProfile().getUser() != null)
                .map(task -> task.getProfile().getUser().getId())
                .distinct()
                .collect(Collectors.toList());

        return new RescheduledTasksResource("RESCHEDULED_TASKS", totalRescheduledTimes, details, rescheduledUserIds);
    }

    @Override
    public TaskDistributionResource handle(GetTaskDistributionInGroupQuery query) {

        List<Task> groupTasks = taskRepository.findByProfile_Group_Id(query.groupId());

        List<User> users = userRepository.findAll();

        Map<Long, List<Task>> tasksByMemberId = groupTasks.stream()
                .filter(task -> task.getProfile().getUser() != null)
                .collect(Collectors.groupingBy(task -> task.getProfile().getUser().getId()));

        Map<String, MemberTaskInfo> details = new HashMap<>();
        for (Map.Entry<Long, List<Task>> entry : tasksByMemberId.entrySet()) {
            Long userId = entry.getKey();
            int taskCount = entry.getValue().size();
            Optional<User> userOpt = users.stream()
                    .filter(u ->u.getId().equals(userId))
                    .findFirst();
            String memberName = userOpt.map(u -> u.getName() + " " + u.getSurname()).orElse("Desconocido");
            details.put(userId.toString(), new MemberTaskInfo(memberName, taskCount));
        }

        return new TaskDistributionResource("TASK_DISTRIBUTION", groupTasks.size(), details);
    }

    @Override
    public TaskOverviewResource handle(GetTaskOverviewInGroupQuery query) {
        List<Task> groupTasks = taskRepository.findByProfile_Group_Id(query.groupId());

        Map<String, Long> overview = groupTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getStatus().name(), Collectors.counting()));

        Map<String, Integer> converted = overview.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()
                ));

        return new TaskOverviewResource("TASK_OVERVIEW", groupTasks.size(), converted);
    }

    @Override
    public TaskOverviewResource handle(GetTaskOverviewForUserInGroupQuery query) {
        List<Task> memberTasks = taskRepository.findByProfile_User_Id(query.memberId());
        Map<String, Long> overview = memberTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getStatus().name(), Collectors.counting()));
        Map<String, Integer> converted = overview.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()
                ));
        return new TaskOverviewResource("TASK_OVERVIEW_MEMBER", memberTasks.size(), converted);
    }

    @Override
    public TaskDistributionResource handle(GetTaskDistributionForUserInGroupQuery query) {
        List<Task> memberTasks = taskRepository.findByProfile_User_Id(query.userId());
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> u.getId().equals(query.userId()))
                .findFirst();
        String memberName = userOpt.map(u -> u.getName() + " " + u.getSurname()).orElse("Desconocido");
        Map<String, MemberTaskInfo> details = Map.of(
                query.userId().toString(), new MemberTaskInfo(memberName, memberTasks.size())
        );
        return new TaskDistributionResource("TASK_DISTRIBUTION_MEMBER", memberTasks.size(), details);
    }

    @Override
    public RescheduledTasksResource handle(GetRescheduledTasksForUserInGroupQuery query) {
        List<Task> memberTasks = taskRepository.findByProfile_User_Id(query.userId());
        long totalRescheduledTimes = memberTasks.stream()
                .mapToLong(Task::getTimesRearranged)
                .sum();
        Map<String, Integer> details = Map.of(
                "total", memberTasks.size(),
                "rescheduled", (int) totalRescheduledTimes
        );
        List<Long> rescheduledMemberIds = totalRescheduledTimes > 0 ? List.of(query.userId()) : List.of();
        return new RescheduledTasksResource("RESCHEDULED_TASKS_MEMBER", totalRescheduledTimes, details, rescheduledMemberIds);
    }

    @Override
    public AvgCompletionTimeResource handle(GetAvgCompletionTimeForUserInGroupQuery query) {
        List<Task> memberTasks = taskRepository.findByProfile_User_Id(query.userId());
        List<Task> completedTasks = memberTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE) // Cambiado a DONE
                .collect(Collectors.toList());
        double avg = completedTasks.stream()
                .mapToLong(Task::getTimePassed)
                .average()
                .orElse(0);
        return new AvgCompletionTimeResource(
                "AVG_COMPLETION_TIME_MEMBER",
                avg / (1000 * 60 * 60 * 24),
                Map.of("completedTasks", completedTasks.size())
        );
    }

    @Override
    public TaskTimePassedResource handle(GetTaskTimePassedByIdQuery query) {
        Optional<Task> taskOpt = taskRepository.findById(query.taskId());
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + query.taskId());
        }

        Task task = taskOpt.get();
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.DONE) {
            throw new IllegalStateException("Task is not completed or done.");
        }

        Long memberId = task.getProfile().getUser() != null ? task.getProfile().getUser().getId() : null;
        long timePassedMillis = task.getTimePassed();

        return new TaskTimePassedResource(memberId, timePassedMillis);
    }

    @Override
    public TaskDurationResource handle(GetTaskDurationByIdQuery query) {
        Optional<Task> taskOpt = taskRepository.findById(query.taskId());
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + query.taskId());
        }

        Task task = taskOpt.get();
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Task is not IN_PROGRESS.");
        }

        // Calcular la duración usando la fecha de creación y la fecha de culminación
        long durationMillis = task.getDueDate().toInstant().toEpochMilli() - task.getCreatedAt().toInstant().toEpochMilli();
        long durationHours = durationMillis / (1000 * 60 * 60);

        return new TaskDurationResource(task.getId(), durationHours);
    }
}