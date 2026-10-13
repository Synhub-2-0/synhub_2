package com.hub.synhub_backend.groups.application.external;

import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.stereotype.Service;

@Service
public class ExternalTasksService {
    private final TasksContextFacade tasksContextFacade;

    public ExternalTasksService(TasksContextFacade tasksContextFacade) {
        this.tasksContextFacade = tasksContextFacade;
    }

    public void deleteTasksByUserIdAndGroupId(Long userId, Long groupId) {
        tasksContextFacade.deleteTasksByUserIdAndGroupId(userId, groupId);
    }
}
