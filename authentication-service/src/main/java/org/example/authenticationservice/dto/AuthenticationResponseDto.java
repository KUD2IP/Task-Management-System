package org.example.authenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "AuthenticationResponseDto", description = "DTO для аутентификации пользователя")
public class AuthenticationResponseDto {

    @Schema(description = "Токен для аутентификации")
    private final String accessToken;

    @Schema(description = "Токен для обновления")
    private final String refreshToken;
}
