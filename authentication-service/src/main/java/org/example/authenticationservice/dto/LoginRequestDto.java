package org.example.authenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "LoginRequestDto", description = "DTO для входа в систему")
public class LoginRequestDto {

    @Email(message = "Email is not valid")
    @NotEmpty(message = "Email cannot be empty")
    @NotNull(message = "Email cannot be null")
    @Schema(description = "Email пользователя", example = "pCfDj@example.com")
    private String email;
    @NotEmpty(message = "Password cannot be empty")
    @NotNull(message = "Password cannot be null")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "Пароль пользователя", example = "12345678")
    private String password;
}
