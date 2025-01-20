package org.example.taskservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.CommentRequestDto;
import org.example.taskservice.dto.CommentResponseDto;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.dto.TaskResponseDto;
import org.example.taskservice.entity.*;
import org.example.taskservice.exeception.InvalidCommentDataException;
import org.example.taskservice.exeception.InvalidTaskDataException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class MapperService {

    /**
     * Метод для преобразования задачи из DTO в сущность Task
     *
     * @param taskRequestDto - DTO задачи
     * @param oldTask        - существующая задача
     * @return - сущность Task
     */
    public Task mapToTask(TaskRequestDto taskRequestDto, Task oldTask) {

        // Если поле не передано, оставляем значение из существующей задачи
        String name = (taskRequestDto.getName() != null && !taskRequestDto.getName().isEmpty())
                ? taskRequestDto.getName()
                : oldTask.getName(); // оставляем старое значение

        String description = (taskRequestDto.getDescription() != null && !taskRequestDto.getDescription().isEmpty())
                ? taskRequestDto.getDescription()
                : oldTask.getDescription(); // оставляем старое значение

        TaskStatus status = taskRequestDto.getStatus();
        // Если статус не передан, оставляем существующий или по умолчанию
        if (taskRequestDto.getStatus() == null) {
            status = (oldTask.getStatus() != null)
                    ? oldTask.getStatus()
                    : TaskStatus.IN_WAITING;
        }

        TaskPriority priority = taskRequestDto.getPriority();
        // Если приоритет не передан, оставляем старое значение
        if (taskRequestDto.getPriority() == null && oldTask.getPriority() == null) {
            throw new InvalidTaskDataException("Priority cannot be null");
        }

        log.info("Task name: {}, description: {}, status: {}, priority: {}", name, description, status, priority);
        // Создаем и возвращаем обновленную задачу
        return Task.builder()
                .id(oldTask.getId())
                .name(name)
                .description(description)
                .status(status)
                .priority(priority)
                .author(oldTask.getAuthor())
                .build();
    }


    /**
     * Метод для преобразования комментария из DTO в сущность Comment
     *
     * @param commentRequestDto - DTO комментария
     * @return - сущность Comment с текстом комментария
     */
    public Comment mapToComment(CommentRequestDto commentRequestDto) {

        // Если поле не передано, выбрасываем исключение
        if(commentRequestDto.getContent() == null || commentRequestDto.getContent().isEmpty()) {
            throw new InvalidCommentDataException("Content cannot be null or empty");
        }

        // Создаем и возвращаем обновленный комментарий
        return Comment.builder()
                .content(commentRequestDto.getContent())
                .build();
    }

    /**
     * Преобразование сущности Task в DTO TaskResponseDto.
     *
     * @param task задача
     * @return TaskResponseDto
     */
    public TaskResponseDto convertToTaskResponseDto(Task task) {
        return TaskResponseDto.builder()
                .id(task.getId()) // Идентификатор задачи
                .name(task.getName()) // Название задачи
                .description(task.getDescription()) // Описание задачи
                .status(task.getStatus()) // Статус задачи
                .priority(task.getPriority())  // Приоритет задачи
                .authorId(task.getAuthor().getId()) // ID автора задачи
                .authorName(task.getAuthor().getName()) // Имя автора задачи
                .executorId(task.getExecutors() != null
                        ? task.getExecutors().stream().map(User::getId).collect(Collectors.toSet())
                        : null) // ID исполнителя
                .executorName(task.getExecutors() != null
                        ? task.getExecutors().stream().map(User::getName).collect(Collectors.toSet())
                        : null) // Имя исполнителя
                .comments(task.getComments().stream().map(this::convertToCommentResponseDto).collect(Collectors.toSet())) // Комментарии
                .build();
    }


    /**
     * Преобразование сущности Comment в DTO CommentResponseDto.
     *
     * @param comment комментарий
     * @return CommentResponseDto
     */
    public CommentResponseDto convertToCommentResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId()) // Идентификатор комментария
                .content(comment.getContent()) // Текст комментария
                .authorId(comment.getAuthor().getId()) // ID автора комментария
                .authorName(comment.getAuthor().getName()) // Имя автора комментария
                .build();
    }
}
