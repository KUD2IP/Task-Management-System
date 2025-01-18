package org.example.taskservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.CommentRequestDto;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.dto.TaskResponseDto;
import org.example.taskservice.entity.Comment;
import org.example.taskservice.entity.Task;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;
import org.example.taskservice.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
@Slf4j
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Метод для создания задачи.
     * Доступно только администратору
     *
     * @param taskRequestDto - DTO задачи
     * @param request - запрос
     * @return - ID созданной задачи
     * @throws IOException - исключение ввода-вывода
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createTask(
            @RequestBody TaskRequestDto taskRequestDto,
            HttpServletRequest request
    ) throws IOException {
        log.info("Received request to create task: {}", taskRequestDto);

        Long taskId = taskService.createTask(taskRequestDto, request);

        log.info("Task created successfully with ID: {}", taskId);

        return ResponseEntity.ok(taskId); // Возвращаем ID созданной задачи с кодом 200 OK
    }


    /**
     * Метод для обновления задачи.
     * Доступно только администратору
     *
     * @param taskId - идентификатор задачи
     * @param taskRequestDto - DTO задачи
     * @return - ID обновленной задачи
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskRequestDto taskRequestDto) {

        log.info("Updating task {}: {}", taskId, taskRequestDto);
        Long updatedTaskId = taskService.updateTask(taskId, taskRequestDto);
        log.info("Task updated successfully with ID: {}", updatedTaskId);

        return ResponseEntity.ok(updatedTaskId);
    }


    /**
     * Метод для обновления статуса задачи.
     * Доступно администратору и исполнителю
     *
     * @param taskId - идентификатор задачи
     * @param status - новый статус
     * @return - идентификатор обновленной задачи
     */
    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_EXECUTOR')")
    public ResponseEntity<Long> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam TaskStatus status) {

        log.info("Updating status: {} for task: {}", status, taskId);
        Long updatedTaskId = taskService.updateTaskStatus(taskId, status);
        log.info("Task status updated successfully with ID: {}", updatedTaskId);

        return ResponseEntity.ok(updatedTaskId);
    }


    /**
     * Метод для обновления приоритета задачи.
     * Доступно только администратору
     *
     * @param taskId - идентификатор задачи
     * @param priority - новый приоритет
     * @return -
     */
    @PatchMapping("/{taskId}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> updateTaskPriority(
            @PathVariable Long taskId,
            @RequestParam TaskPriority priority) {

        log.info("Updating priority: {} for task: {}", priority, taskId);
        Long updatedTaskId = taskService.updateTaskPriority(taskId, priority);
        log.info("Task priority updated successfully with ID: {}", updatedTaskId);

        return ResponseEntity.ok(updatedTaskId);
    }


    // Назначение исполнителя задачи - доступно только администратору
    @PatchMapping("/executor/{userId}/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> updateTaskExecutor(@PathVariable Long taskId, @PathVariable Long userId) {
        Long task = taskService.updateTaskExecutor(taskId, userId);
        return ResponseEntity.ok(task);
    }


    /**
     * Метод для удаления задачи.
     * Доступно только администратору
     *
     * @param taskId - идентификатор задачи
     * @return - сообщение об успешном удалении
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }

    /**
     * Метод для добавления комментария к задаче.
     * Доступно только администратору и исполнителю
     *
     * @param taskId - идентификатор задачи
     * @param commentRequestDto - данные комментария
     * @param request - HTTP-запрос (токен пользователя)
     * @return - сообщение об успешном добавлении
     * @throws IOException - исключение
     */
    @PatchMapping("/{taskId}/comment")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_EXECUTOR')")
    public ResponseEntity<String> addComment(
            @PathVariable Long taskId,
            @RequestBody CommentRequestDto commentRequestDto,
            HttpServletRequest request
            ) throws IOException {

        taskService.addComment(taskId, commentRequestDto, request);

        return ResponseEntity.ok("Comment added");
    }


    /**
     * Получение задач по автору с фильтрацией и пагинацией.
     *
     * @param userId - идентификатор пользователя
     * @param page - номер страницы
     * @param size - размер страницы
     * @param status - статус задачи
     * @param priority - приоритет задачи
     * @return - задачи по автору
     */
    @GetMapping("/author/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TaskResponseDto>> getTasksByAuthor(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {

        log.info("Fetching tasks for author {} with filters - status: {}, priority: {}", userId, status, priority);
        // Поиск задач по идентификатору пользователя
        Page<TaskResponseDto> tasks = taskService.getTasksByAuthor(userId, page, size, status, priority);
        return ResponseEntity.ok(tasks);
    }


    // Получение задачи по ID
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> findTaskById(@PathVariable Long taskId) {
        Task task = taskService.findTaskById(taskId);
        return ResponseEntity.ok(task);
    }

    // Получение всех задач с фильтрацией и пагинацией
    @GetMapping
    public ResponseEntity<Page<Task>> findAllTasks(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> priority,
            @RequestParam int page,
            @RequestParam int size
    ) {
        Page<Task> tasks = taskService.findTasks(status, priority, page, size);
        return ResponseEntity.ok(tasks);
    }


    // Получение задач по ID исполнителя
    @GetMapping("/executor/{userId}")
    public ResponseEntity<Page<Task>> findAllTasksByExecutorId(@PathVariable Long userId) {
        Page<Task> tasks = taskService.findTasksByExecutorId(userId);
        return ResponseEntity.ok(tasks);
    }

    // Получение всех комментариев по задаче
    @GetMapping("/comment/{taskId}")
    public ResponseEntity<List<Comment>> findAllCommentsByTaskId(@PathVariable Long taskId) {
        List<Comment> comments = taskService.findCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }
}

