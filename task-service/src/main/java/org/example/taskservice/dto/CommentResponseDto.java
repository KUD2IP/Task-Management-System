package org.example.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "DTO комментария")
public class CommentResponseDto {

    @Schema(description = "ID комментария")
    private Long id; // ID комментария

    @Schema(description = "Текст комментария")
    private String content; // Текст комментария

    @Schema(description = "ID автора комментария")
    private Long authorId; // ID автора комментария

    @Schema(description = "Имя автора комментария")
    private String authorName; // Имя автора комментария
}
