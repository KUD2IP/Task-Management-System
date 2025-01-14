package org.example.authenticationservice.dto;

import lombok.Getter;

@Getter
public class AuthenticationResponseDto {

    private final String accessToken;

    private final String refreshToken;

    public AuthenticationResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
