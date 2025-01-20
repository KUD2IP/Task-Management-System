package org.example.taskservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.CommentRequestDto;
import org.example.taskservice.dto.CommentResponseDto;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.dto.TaskResponseDto;
import org.example.taskservice.entity.*;
import org.example.taskservice.exeception.AccessDeniedException;
import org.example.taskservice.exeception.TaskNotFoundException;
import org.example.taskservice.exeception.UserNotFoundException;
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
        log.info("Created task: {}, with author: {}", task.getName(), user.getEmail());

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
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
        log.info("Found task: {}", task.getName());
        // Обновление задачи
        task = mapperService.mapToTask(taskRequestDto, task);
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
    public Long updateTaskStatus(Long taskId, TaskStatus status, HttpServletRequest request) throws IOException {
        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        User executor = verificationOfAuthorship(task, request);

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
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
        // Обновление приоритета задачи
        task.setPriority(priority);
        log.info("Updated task priority: {}", task);
        // Сохранение обновленной задачи
        return taskRepository.save(task).getId();
    }


    /**
     * Метод для добавления исполнителя задачи
     *
     * @param taskId - идентификатор задачи
     * @param userId - идентификатор пользователя
     * @param request - токен пользователя
     * @return - идентификатор обновленной задачи
     */
    public Long updateTaskExecutor(Long taskId, Long userId, HttpServletRequest request) {

        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        // Изменение роли в auth-service и получение обновленного пользователя
        User user = userService.saveExecutor(userId, request);

        // Добавление исполнителя в задачу
        Set<User> executors = task.getExecutors();
        executors.add(user);
        task.setExecutors(executors);

        // Сохранение обновленной задачи
        return taskRepository.save(task).getId();
    }


    /**
     * Метод для удаления задачи
     *
     * @param taskId - идентификатор задачи
     */
    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    private User verificationOfAuthorship(Task task, HttpServletRequest request) throws IOException {
        // Узнаем данные пользователя из токена
        User executor = userService.getClaimsFromToken(request);

        // Ищем пользователя в базе данных
        executor = userRepository.findByEmail(executor.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        // Проверяем, является ли пользователь автором задачи
        User finalExecutor = executor;

        if(task.getExecutors().stream().noneMatch(user -> user.getEmail().equals(finalExecutor.getEmail()))) {
            log.info("You are not the author of the task");
            throw new AccessDeniedException("You are not the author of the task");
        }

        return executor;
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
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        User finalExecutor = verificationOfAuthorship(task, request);

        newComment.setTask(task); // Привязка комментария к задаче
        newComment.setAuthor(finalExecutor); // Привязка комментария к автору задачи

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

        log.info("Fetching tasks for author {} ", authorId);

        Pageable pageable = PageRequest.of(page, size);

        // Если фильтры не переданы, возвращаем все задачи автора
        Specification<Task> specification = buildSpecification(authorId, null, status, priority);

        // Поиск задач
        Page<Task> tasks = taskRepository.findAll(specification, pageable);

        // Конвертируем задачи в DTO перед возвратом
        return tasks.map(mapperService::convertToTaskResponseDto);
    }


    /**
     * Метод для получения задачи по идентификатору
     *
     * @param taskId - идентификатор задачи
     * @return - задачу по идентификатору
     */
    public TaskResponseDto getTaskById(Long taskId) {

        log.info("Fetching task with ID: {}", taskId);

        // Поиск задачи по идентификатору
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        // Конвертируем задачи в DTO перед возвратом
        return mapperService.convertToTaskResponseDto(task);
    }


    /**
     * Метод для получения всех задач с фильтрацией и пагинацией
     *
     * @param status - статус задачи
     * @param priority - приоритет задачи
     * @param page - номер страницы
     * @param size - размер страницы
     * @return - все задачи с фильтрацией и пагинацией
     */
    public Page<TaskResponseDto> getAllTasks(
            TaskStatus status,
            TaskPriority priority,
            int page,
            int size) {

        log.info("Fetching all tasks");

        Pageable pageable = PageRequest.of(page, size);

        //Добавляем фильтры
        Specification<Task> specification = buildSpecification(null, null, status, priority);

        //Получаем все задачи
        Page<Task> tasks = taskRepository.findAll(specification, pageable);

        // Добавляем фильтры и конвертируем задачи в DTO перед возвратом
        return tasks.map(mapperService::convertToTaskResponseDto);

    }

    /**
     * Метод для добавления фильтров по статусу и приоритету
     *
     * @param status - статус
     * @param priority - приоритет
     * @return - задачи с фильтром
     */
    private Specification<Task> buildSpecification(
            Long authorId,
            Long executorId,
            TaskStatus status,
            TaskPriority priority) {
        Specification<Task> specification = Specification.where(null);

        if (authorId != null) {
            specification = specification.and(TaskSpecification.hasAuthorId(authorId));
        }
        if (executorId != null) {
            specification = specification.and(TaskSpecification.hasExecutorId(executorId));
        }
        if (status != null) {
            specification = specification.and(TaskSpecification.hasStatus(status));
        }
        if (priority != null) {
            specification = specification.and(TaskSpecification.hasPriority(priority));
        }

        return specification;
    }


    /**
     * Метод для получения задачи по идентификатору исполнителя
     *
     * @param userId - идентификатор исполнителя
     * @param page - номер страницы
     * @param size - размер страницы
     * @param status - статус задачи
     * @param priority - приоритет задачи
     * @return - задачи по идентификатору исполнителя
     */
    public Page<TaskResponseDto> getTasksByExecutorId(
            Long userId,
            int page,
            int size,
            TaskStatus status,
            TaskPriority priority) {

        log.info("Fetching tasks for executor {}", userId);

        Pageable pageable = PageRequest.of(page, size);

        // Добавляем фильтры
        Specification<Task> specification = buildSpecification(null, userId, status, priority);

        // Поиск задач
        Page<Task> tasks = taskRepository.findAll(specification, pageable);

        // Добавляем фильтры и конвертируем задачи в DTO перед возвратом
        return tasks.map(mapperService::convertToTaskResponseDto);
    }


    /**
     * Метод для получения комментариев по идентификатору задачи
     *
     * @param taskId - идентификатор задачи
     * @return - список комментариев
     */
    public Page<CommentResponseDto> getCommentsByTaskId(Long taskId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Comment> specification = CommentSpecification.hasTaskId(taskId);

        Page<Comment> comments = commentRepository.findAll(specification, pageable);

        return comments.map(mapperService::convertToCommentResponseDto);
    }
}