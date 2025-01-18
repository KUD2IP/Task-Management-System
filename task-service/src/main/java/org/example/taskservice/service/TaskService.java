package org.example.taskservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.CommentRequestDto;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.dto.TaskResponseDto;
import org.example.taskservice.entity.*;
import org.example.taskservice.repository.CommentRepository;
import org.example.taskservice.repository.TaskRepository;
import org.example.taskservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MapperService mapperService;

    public TaskService(TaskRepository taskRepository,
                       CommentRepository commentRepository,
                       UserRepository userRepository,
                       UserService userService, MapperService mapperService) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.mapperService = mapperService;
    }

    /**
     * Метод для создания задачи.
     *
     * @param taskRequestDto - данные задачи (название, описание, статус, приоритет)
     * @param request - запрос
     * @throws IOException - исключение ввода-вывода
     */
    public Long createTask(TaskRequestDto taskRequestDto, HttpServletRequest request) throws IOException {

        // Получаем данные пользователя из токена
        User user = userService.getClaimsFromToken(request);

        // Проверка на наличие пользователя в базе данных
        if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
            log.info("User not found in database, saving user: {}", user);
            userService.saveUser(user);  // Сохраняем пользователя в базу данных
        } else {
            log.info("User already exists: {}", user);
        }

        user = userRepository.findByEmail(user.getEmail()).get();

        // Создаем новую задачу
        Task task = new Task();

        // Заполняем поля задачи
        task = mapperService.mapToTask(taskRequestDto, task);

        task.setAuthor(user);  // Автор задачи

        // Логирование информации о задаче
        log.info("Created task: {}, with author: {}", task, user.getEmail());

        // Сохраняем задачу в базе данных
        return taskRepository.save(task).getId();
    }

    /**
     * Метод для обновления задачи.
     *
     * @param taskId - идентификатор задачи
     * @param taskRequestDto - новые данные задачи
     * @return - идентификатор обновленной задачи
     */
    public Long updateTask(Long taskId, TaskRequestDto taskRequestDto) {
        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        log.info("Found task: {}", task.getName());
        // Обновление задачи
        task = mapperService.mapToTask(taskRequestDto, task);
        log.info("Updated task: {}", task.getName());
        // Сохранение обновленной задачи
        return taskRepository.save(task).getId();
    }

    /**
     * Метод для обновления статуса задачи.
     *
     * @param taskId - идентификатор задачи
     * @param status - новый статус
     * @return - идентификатор обновленной задачи
     */
    public Long updateTaskStatus(Long taskId, TaskStatus status) {
        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        // Обновление статуса задачи
        task.setStatus(status);
        log.info("Updated task status: {}", task);
        // Сохранение обновленной задачи
        return taskRepository.save(task).getId();
    }

    /**
     * Метод для обновления приоритета задачи.
     *
     * @param taskId - идентификатор задачи
     * @param priority - новый приоритет
     * @return - идентификатор обновленной задачи
     */
    public Long updateTaskPriority(Long taskId, TaskPriority priority) {
        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        // Обновление приоритета задачи
        task.setPriority(priority);
        log.info("Updated task priority: {}", task);
        // Сохранение обновленной задачи
        return taskRepository.save(task).getId();
    }

    public Long updateTaskExecutor(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<User> executors = task.getExecutors();
        executors.add(user);
        task.setExecutors(executors);

        return taskRepository.save(task).getId();
    }

    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    /**
     * Метод для добавления комментария к задаче
     *
     * @param taskId - идентификатор задачи
     * @param commentRequestDto - данные комментария
     * @param request - токен пользователя
     * @throws IOException - исключение
     */
    public void addComment(
            Long taskId,
            CommentRequestDto commentRequestDto,
            HttpServletRequest request
    ) throws IOException {
        // Маппинг данных комментария
        Comment newComment = mapperService.mapToComment(commentRequestDto);
        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        // Узнаем данные пользователя из токена
        User author = userService.getClaimsFromToken(request);

        // Ищем пользователя в базе данных
        author = userRepository.findByEmail(author.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем, является ли пользователь автором задачи
        if(!task.getAuthor().getEmail().equals(author.getEmail())) {
            log.info("You are not the author of the task");
            throw new RuntimeException("You are not the author of the task");
        }

        newComment.setTask(task); // Привязка комментария к задаче
        newComment.setAuthor(author); // Привязка комментария к автору задачи

        // Сохранение комментария
        commentRepository.save(newComment);
    }


    /**
     * Получение задач автора с фильтрацией и пагинацией.
     *
     * @param authorId ID автора задач
     * @param page номер страницы (для пагинации)
     * @param size размер страницы (для пагинации)
     * @param status статус задачи для фильтрации (опционально)
     * @param priority приоритет задачи для фильтрации (опционально)
     * @return задачи автора в виде страницы
     */
    public Page<TaskResponseDto> getTasksByAuthor(
            Long authorId,
            int page,
            int size,
            TaskStatus status,
            TaskPriority priority) {

        log.info("Fetching tasks for author {} with filters - status: {}, priority: {}", authorId, status, priority);

        Pageable pageable = PageRequest.of(page, size);

        // Если фильтры не переданы, возвращаем все задачи автора
        Specification<Task> specification = Specification.where(TaskSpecification.hasAuthorId(authorId));

        if (status != null) {
            specification = specification.and(TaskSpecification.hasStatus(status));
        }

        if (priority != null) {
            specification = specification.and(TaskSpecification.hasPriority(priority));
        }

        // Выполняем запрос к репозиторию
        Page<Task> tasks = taskRepository.findAll(specification, pageable);

        // Конвертируем задачи в DTO перед возвратом
        return tasks.map(mapperService::convertToTaskResponseDto);
    }

    public Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public Page<Task> findTasks(Optional<String> status, Optional<String> priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status.isPresent() && priority.isPresent()) {
            return taskRepository.findByStatusAndPriority(status.get(), priority.get(), pageable);
        } else if (status.isPresent()) {
            return taskRepository.findByStatus(status.get(), pageable);
        } else if (priority.isPresent()) {
            return taskRepository.findByPriority(priority.get(), pageable);
        } else {
            return taskRepository.findAll(pageable);
        }
    }

    public Page<Task> findTasksByAuthorId(Long userId) {
        return taskRepository.findByAuthorId(userId, PageRequest.of(0, 10));
    }

    public Page<Task> findTasksByExecutorId(Long userId) {
        return taskRepository.findByExecutorsId(userId, PageRequest.of(0, 10));
    }

    public List<Comment> findCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskId(taskId);
    }

}