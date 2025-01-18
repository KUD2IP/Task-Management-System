package org.example.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "TaskRequestDto", description = "DTO для создания задачи")
public class TaskRequestDto {

    @Schema(description = "Название задачи", example = "Implement new feature")
    private String name;

    @Schema(description = "Описание задачи", example = "Implement a new feature in the task management system")
    private String description;

    @Schema(description = "Статус задачи", allowableValues = {"TO_DO", "IN_PROGRESS", "DONE"})
    private TaskStatus status;

    @Schema(description = "Приоритет задачи", allowableValues = {"LOW", "MEDIUM", "HIGH"})
    private TaskPriority priority;
}
