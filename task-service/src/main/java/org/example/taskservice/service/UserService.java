package org.example.taskservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.dto.UserRequestDto;
import org.example.taskservice.entity.User;
import org.example.taskservice.exeception.UserAlreadyExistsException;
import org.example.taskservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    @Value("${security.jwt.secret_key}")
    private String secretKey;
    private final RestTemplate restTemplate;

    public UserService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Метод для сохранения пользователя в базу данных.
     *
     * @param user Пользователь для сохранения
     */
    public void saveUser(User user) {
        // Проверка наличия пользователя в базе данных
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
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

        log.info("Found email: {}, name: {}, role: {}", email, name, role.toString());

        // Запись пользователя
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role.toString().replace("[", "").replace("]", ""));

        return user;
    }

    /**
     *  Метод для обновления пользователя в базе данных.
     *  Присвоение executor роли
     *
     * @param userId - ID пользователя
     * @param request - HTTP-запрос
     * @return HTTP-ответ от сервиса auth-service
     */
    private ResponseEntity<String> updateExecutor(Long userId, HttpServletRequest request) {
        //Получение токена
        String headers = request.getHeader(HttpHeaders.AUTHORIZATION);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        //Запрос на обновление executor роли
        String url = String.format("http://localhost:8222/api/v1/auth/executor/%s", userId);
        return restTemplate.postForEntity(url, null, String.class);
    }

    /**
     *  Метод для получения обновленного пользователя в базе данных.
     *
     * @param userId - ID пользователя
     * @param request - HTTP-запрос
     * @return HTTP-ответ от сервиса auth-service
     */
    private ResponseEntity<UserRequestDto> updateExecutorCredentials(Long userId, HttpServletRequest request) {
        //Получение токена
        String headers = request.getHeader(HttpHeaders.AUTHORIZATION);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        //Запрос на получение обновленного пользователя
        String url = String.format("http://localhost:8222/api/v1/auth/executor/%s", userId);
        log.info("Updating executor credentials: {}", url);
        return restTemplate.getForEntity(url, UserRequestDto.class);
    }

    /**
     *  Метод для сохранения обновленного пользователя в базе данных с executor ролью.
     *
     * @param userId - ID пользователя
     * @param request - HTTP-запрос
     * @return обновленного пользователя
     */
    public User saveExecutor(Long userId, HttpServletRequest request) {

        //Создание пользователя
        User user = new User();

        //Получение обновленного пользователя
        updateExecutor(userId, request).getBody();
        UserRequestDto userUpdated = updateExecutorCredentials(userId, request).getBody();

        //Заполнение полей
        user.setEmail(userUpdated.getEmail());
        user.setName(userUpdated.getName());
        user.setRole(userUpdated.getRole());

        log.info("Saving user: with email: {}, name: {}, role: {}", user.getEmail(), user.getName(), user.getRole());

        //Сохранение пользователя
        userRepository.save(user);

        return user;
    }
}
