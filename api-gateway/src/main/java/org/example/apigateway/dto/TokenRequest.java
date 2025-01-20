package org.example.apigateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TokenRequest", description = "DTO для обновления токена")
public class TokenRequest {

    @Schema(description = "Токен для аутентификации")
    private String token;
}

