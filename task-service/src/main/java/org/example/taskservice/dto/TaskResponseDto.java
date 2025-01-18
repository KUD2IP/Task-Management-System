package org.example.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;

import java.util.Set;

@Data
@Builder
@Schema(description = "DTO задачи")
public class TaskResponseDto {

    @Schema(description = "Уникальный идентификатор задачи")
    private Long id; // Уникальный идентификатор задачи

    @Schema(description = "Название задачи")
    private String name; // Название задачи

    @Schema(description = "Описание задачи")
    private String description; // Описание задачи

    @Schema(description = "Статус задачи")
    private TaskStatus status; // Статус задачи (TO_DO, IN_PROGRESS, DONE)

    @Schema(description = "Приоритет задачи")
    private TaskPriority priority; // Приоритет задачи (LOW, MEDIUM, HIGH)

    @Schema(description = "ID автора задачи")
    private Long authorId; // ID автора задачи

    @Schema(description = "Имя автора задачи")
    private String authorName; // Имя автора задачи

    @Schema(description = "ID исполнителя задачи")
    private Set<Long> executorId; // ID исполнителя задачи

    @Schema(description = "Имя исполнителя задачи")
    private Set<String> executorName; // Имя исполнителя задачи

    @Schema(description = "Комментарии задачи")
    private Set<CommentResponseDto> comments; // Комментарии задачи
}
