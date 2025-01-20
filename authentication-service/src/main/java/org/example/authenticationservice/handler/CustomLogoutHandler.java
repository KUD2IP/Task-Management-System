package org.example.authenticationservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.entity.Token;
import org.example.authenticationservice.repository.TokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenRepository tokenRepository;

    public CustomLogoutHandler(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }


    /**
     * Метод для выхода пользователя из системы.
     * Обрабатывает входящие запросы и обнуляет состояние токена в хранилище.
     *
     * @param request - запрос
     * @param response - ответ
     * @param authentication - аутентификация
     */
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
        Token tokenEntity = tokenRepository.findByAccessToken(token)
                .orElse(null);

        // Если токен найден, устанавливаем флаг "loggedOut" в true
        if (tokenEntity != null) {
            tokenEntity.setLoggedOut(true);
            tokenRepository.save(tokenEntity);
        }
    }
}
