package org.example.authenticationservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.dto.AuthenticationResponseDto;
import org.example.authenticationservice.dto.LoginRequestDto;
import org.example.authenticationservice.dto.RegistrationRequestDto;
import org.example.authenticationservice.entity.BlackList;
import org.example.authenticationservice.entity.RefreshToken;
import org.example.authenticationservice.entity.Role;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.repository.BlackListRepository;
import org.example.authenticationservice.repository.RefreshTokenRepository;
import org.example.authenticationservice.repository.RoleRepository;
import org.example.authenticationservice.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BlackListRepository blackListRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthenticationService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 BlackListRepository blackListRepository,
                                 JwtService jwtService,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.blackListRepository = blackListRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param request Запрос на регистрацию, содержащий данные пользователя.
     */
    public void register(RegistrationRequestDto request) {
        log.info("Starting registration for email: {}", request.getEmail());

        // Создание нового пользователя
        User user = new User();

        // Поиск пользователя по email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User with email {} already exists", request.getEmail());
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }

        // Поиск роли пользователя (в данном случае роль USER)
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> {
                    log.error("Role USER not found");
                    return new RuntimeException("Role USER not found");
                });

        // Заполнение данных пользователя
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // Хеширование пароля
        user.setRoles(Set.of(userRole));  // Устанавливаем роль

        // Сохранение нового пользователя в базе данных
        user = userRepository.save(user);

        log.info("User registered successfully with email: {}", user.getEmail());
    }

    /**
     * Авторизация пользователя. Генерация токенов для доступа и обновления.
     *
     * @param request Данные пользователя для входа (email, пароль).
     * @return Объект с двумя токенами: access и refresh.
     */
    public AuthenticationResponseDto authenticate(LoginRequestDto request) {
        log.info("Attempting to authenticate user with email: {}", request.getEmail());

        // Аутентификация пользователя с использованием менеджера аутентификации
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Поиск пользователя по email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", request.getEmail());
                    return new RuntimeException("User not found");
                });

        // Генерация токенов
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Сохранение refresh токена в базе данных
        saveUserToken(refreshToken, user);

        log.info("Authentication successful for user: {}", user.getEmail());

        // Возвращаем объект с access и refresh токенами
        return new AuthenticationResponseDto(accessToken, refreshToken);
    }

    /**
     * Сохраняет токен refresh в базе данных.
     *
     * @param refreshToken Токен обновления.
     * @param user Пользователь, для которого генерируется токен.
     */
    private void saveUserToken(String refreshToken, User user) {
        log.info("Saving refresh token for user: {}", user.getEmail());

        // Создаем объект токена
        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUser(user);
        token.setExpiryDate(jwtService.calculateExpirationDate(refreshToken));  // Рассчитываем время истечения

        // Сохраняем токен в базе данных
        try {
            refreshTokenRepository.save(token);
            log.info("Refresh token saved successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to save refresh token for user: {}", user.getEmail(), e);
        }
    }

    /**
     * Обновляет токены для пользователя при успешной валидации refresh токена.
     *
     * @param request HTTP-запрос, содержащий старый refresh токен.
     * @param response HTTP-ответ для отправки нового токена.
     * @return Новый объект с токенами (access и refresh), если refresh токен валиден.
     */
    @Transactional
    public ResponseEntity<AuthenticationResponseDto> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Извлекаем заголовок авторизации из запроса
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Проверяем наличие и корректность заголовка авторизации
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Извлекаем токен из заголовка
        String token = authorizationHeader.substring(7);
        log.info("Received refresh token: {}", token);

        // Извлекаем email пользователя из токена
        String email = jwtService.extractUsername(token);

        // Находим пользователя по email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("No user found for email: {}", email);
                    return new UsernameNotFoundException("No user found");
                });

        // Проверяем, является ли refresh токен валидным
        if (jwtService.isValidRefresh(token, user)) {
            // Генерация новых токенов (access и refresh)
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("Generating new access token and refresh token for user: {}", user.getEmail());

            // Добавляем старый токен в черный список
            BlackList blackList = new BlackList();
            blackList.setAccessToken(token);
            blackListRepository.save(blackList);

            // Удаляем старый refresh токен из базы данных
            try {
                refreshTokenRepository.deleteByToken(token);
                log.info("Old refresh token deleted successfully.");
            } catch (Exception e) {
                log.error("Failed to delete old refresh token: {}", token, e);
            }

            // Сохраняем новый refresh токен в базе данных
            saveUserToken(refreshToken, user);

            // Возвращаем новый объект с токенами
            return new ResponseEntity<>(new AuthenticationResponseDto(accessToken, refreshToken), HttpStatus.OK);
        }

        // В случае невалидного refresh токена удаляем его из базы данных
        log.warn("Invalid refresh token: {}", token);
        refreshTokenRepository.deleteByToken(token);

        // Возвращаем ответ с кодом 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
