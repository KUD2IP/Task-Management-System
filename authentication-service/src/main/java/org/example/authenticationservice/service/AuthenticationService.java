package org.example.authenticationservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.dto.AuthenticationResponseDto;
import org.example.authenticationservice.dto.LoginRequestDto;
import org.example.authenticationservice.dto.RegistrationRequestDto;
import org.example.authenticationservice.entity.Token;
import org.example.authenticationservice.entity.Role;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.exception.RoleNotFoundException;
import org.example.authenticationservice.exception.TokenSaveException;
import org.example.authenticationservice.exception.UserNotFoundException;
import org.example.authenticationservice.repository.TokenRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;

    public AuthenticationService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 JwtService jwtService,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
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
            throw new UserNotFoundException("User with email " + request.getEmail() + " already exists");
        }

        // Поиск роли пользователя (в данном случае роль USER)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> {
                    log.error("Role USER not found");
                    return new RoleNotFoundException("Role USER not found");
                });

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(userRole);
        // Заполнение данных пользователя
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // Хеширование пароля
        user.setRoles(roles);  // Устанавливаем роль

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
                    return new UserNotFoundException("User not found");
                });

        // Генерация токенов
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Сохранение refresh токена в базе данных
        saveUserToken(accessToken, refreshToken, user);

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
    private void saveUserToken(String accessToken, String refreshToken, User user) {
        log.info("Saving refresh token for user: {}", user.getEmail());

        // Создаем объект токена
        Token token = new Token();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setUser(user);
        token.setLoggedOut(false);

        // Сохраняем токен в базе данных
        try {
            tokenRepository.save(token);
            log.info("Access and refresh tokens saved successfully for user: {}", user.getEmail());
        } catch (TokenSaveException e) {
            log.error("Failed to save tokens for user: {}", user.getEmail(), e);
            throw new TokenSaveException("Failed to save tokens for user: " + user.getEmail());
        }
    }

    /**
     * Метод отзывает все действительные токены для данного пользователя.
     *
     * @param user Пользователь, для которого нужно отменить токены.
     */
    private void revokeAllToken(User user) {
        // Получаем список всех действительных токенов для данного пользователя
        List<Token> validTokens = tokenRepository.findAllAccessTokenByUser(user.getId());

        // Если список не пустой, то отменяем все токены
        if(!validTokens.isEmpty()){
            validTokens.forEach(t ->{
                // Устанавливаем признак "отменен" для каждого токена
                t.setLoggedOut(true);
            });
        }
        // Сохраняем измененные токены в базе данных
        tokenRepository.saveAll(validTokens);
    }

    /**
     * Обновляет токены для пользователя при успешной валидации refresh токена.
     *
     * @param request HTTP-запрос, содержащий старый refresh токен.
     * @param response HTTP-ответ для отправки нового токена.
     * @return Новый объект с токенами (access и refresh), если refresh токен валиден.
     */
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

            //Отзываем все действительные токены
            revokeAllToken(user);
            log.info("All tokens revoked for user: {}", user.getEmail());

            // Сохраняем новые токены в базе данных
            saveUserToken(accessToken, refreshToken, user);

            // Возвращаем новый объект с токенами
            return new ResponseEntity<>(new AuthenticationResponseDto(accessToken, refreshToken), HttpStatus.OK);
        }

        // Возвращаем ответ с кодом 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Метод для назначения роли EXECUTOR пользователю.
     *
     * @param userId ID пользователя.
     */
    public void assignRoleToUser(Long userId) {

        // Поиск пользователя по ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        // Поиск роли пользователя (в данном случае роль EXECUTOR)
        Role userRole = roleRepository.findByName("ROLE_EXECUTOR")
                .orElseThrow(() -> {
                    log.error("Role EXECUTOR not found");
                    return new RoleNotFoundException("Role EXECUTOR not found");
                });

        // Очистка ролей
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.clear();

        // Добавление роли EXECUTOR
        roles.add(userRole);
        user.setRoles(roles);

        // Сохранение изменений
        userRepository.save(user);

        // Генерация новых токенов для пользователя
        // Проверка наличия действительных токенов
        if(!tokenRepository.findAllAccessTokenByUser(user.getId()).isEmpty()){
            // Генерация новых токенов (access и refresh)
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("Generating new access token and refresh token for user: {}", user.getEmail());

            //Отзываем все действительные токены
            revokeAllToken(user);
            log.info("All tokens revoked for user: {}", user.getEmail());
            // Сохраняем новые токены в базе данных
            saveUserToken(accessToken, refreshToken, user);
        }

    }
}
