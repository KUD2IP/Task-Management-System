package org.example.taskservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.entity.Task;
import org.example.taskservice.entity.User;
import org.example.taskservice.repository.TaskRepository;
import org.example.taskservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import static org.example.taskservice.entity.TaskPriority.MEDIUM;
import static org.example.taskservice.entity.TaskStatus.IN_WAITING;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;

    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public void createTask(TaskRequestDto taskRequestDto, User author) {
        log.info("Creating task");

        Task task = new Task();
        task.setName(taskRequestDto.getName());
        task.setDescription(taskRequestDto.getDescription());
        task.setStatus(IN_WAITING.toString());
        task.setPriority(MEDIUM.toString());
        task.setAuthor(author);

        taskRepository.save(task);
    }
}
