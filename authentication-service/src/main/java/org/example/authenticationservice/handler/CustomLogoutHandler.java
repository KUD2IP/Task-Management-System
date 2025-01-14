package org.example.authenticationservice.handler;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authenticationservice.entity.BlackList;
import org.example.authenticationservice.entity.RefreshToken;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.repository.BlackListRepository;
import org.example.authenticationservice.repository.RefreshTokenRepository;
import org.example.authenticationservice.repository.UserRepository;
import org.example.authenticationservice.service.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutHandler implements LogoutHandler {

    private final BlackListRepository blackListRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    public CustomLogoutHandler(BlackListRepository blackListRepository, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, JwtService jwtService) {
        this.blackListRepository = blackListRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {

        // Получаем заголовок Authorization
        String authHeader = request.getHeader("Authorization");

        // Если заголовок не содержит JWT-токена, пропускаем фильтр
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        // Извлекаем JWT-токен из заголовка
        String token = authHeader.substring(7);

        // Ищем токен в хранилище
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(null);

        blackListRepository.save(BlackList.builder().accessToken(token).build());

        // Если токен найден, устанавливаем флаг "loggedOut" в true
        if (refreshToken != null) {
            refreshTokenRepository.deleteByToken(refreshToken.getToken());
        }
    }
}
