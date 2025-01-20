package org.example.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO комментария")
public class CommentRequestDto {

    @Schema(description = "Текст комментария")
    private String content;
}
