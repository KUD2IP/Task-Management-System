package org.example.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO пользователя")
public class UserRequestDto {

    @Schema(description = "Имя пользователя")
    @NotNull
    @NotEmpty
    private String name;

    @Schema(description = "Электронная почта пользователя")
    @NotNull
    @NotEmpty
    private String email;

    @Schema(description = "Роль пользователя")
    @NotNull
    @NotEmpty
    private String role;
}
