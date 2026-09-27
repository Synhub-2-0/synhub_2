package com.hub.synhub_backend.tasks.aplication.internal.queryservices;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.domain.services.TaskQueryService;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskRepository taskRepository;

    public TaskQueryServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Override
    public List<Task> handle(GetAllTasksQuery query) {
        return this.taskRepository.findAll();
    }

    @Override
    public Optional<Task> handle(GetTaskByIdQuery query) {
        return this.taskRepository.findById(query.taskId());
    }

    @Override
    public List<Task> handle(GetAllTasksByUserId query) {
        return this.taskRepository.findByProfile_User_Id(query.userId());
    }

    @Override
    public List<Task> handle(GetAllTaskByStatusQuery query) {
        TaskStatus taskStatus = TaskStatus.valueOf(query.taskStatus());
        return taskRepository.findByStatus(taskStatus);
    }

    @Override
    public List<Task> handle(GetAllTasksByGroupIdQuery query) {
        return taskRepository.findByProfile_Group_Id(query.groupId());
    }

    @Override
    public List<Task> handle(GetTasksByGroupIdAndStatus query) {
        if (query.status() == null) {
            return taskRepository.findByProfile_Group_Id(query.groupId());
        }
        return taskRepository.findByProfile_Group_IdAndStatus(query.groupId(), query.status());
    }
}
