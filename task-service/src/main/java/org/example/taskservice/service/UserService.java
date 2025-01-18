package org.example.taskservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.entity.User;
import org.example.taskservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    @Value("${security.jwt.secret_key}")
    private String secretKey;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Метод для сохранения пользователя в базу данных.
     *
     * @param user Пользователь для сохранения
     */
    public void saveUser(User user) {
        // Проверка наличия пользователя в базе данных
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        log.info("Saving user: {}", user);
        userRepository.save(user);
    }

    /**
     * Метод для получения пользователя из токена.
     *
     * @param request HTTP-запрос
     * @return Пользователь
     */
    public User getClaimsFromToken(HttpServletRequest request) throws IOException {
        // Извлекаем заголовок авторизации из запроса
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Проверяем наличие и корректность заголовка авторизации
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid.");
            return null;
        }

        // Извлекаем токен из заголовка
        String token = authorizationHeader.substring(7);

        JwtParserBuilder parser = Jwts.parser();

        // Проверка подписи токена
        parser.verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)));

        log.info("Parsing token: {}", token);

        // Парсинг токена
        Claims claims = parser.build()
                .parseSignedClaims(token)
                .getPayload();

        // Получение данных из токена
        String email = claims.getSubject();
        String name = claims.get("name", String.class);
        List<String> role = claims.get("roles", List.class);

        log.info("Found email: {}, name: {}, role: {}", email, name, role.stream().toString());

        // Запись пользователя
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role.toString().replace("[", "").replace("]", ""));

        return user;
    }
}
