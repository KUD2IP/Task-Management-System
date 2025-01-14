package org.example.authenticationservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
     * @param request запрос на регистрацию
     *
     */
    public void register(RegistrationRequestDto request) {
        // Создание нового пользователя
        User user = new User();

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        // Заполнение полей пользователя
        user.setEmail(request.getEmail()); // устанавливаем электронную почту пользователя
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // устанавливаем пароль пользователя
        user.setRoles(Set.of(userRole)); // устанавливаем роль пользователя

        // Сохранение пользователя в базе данных
        user = userRepository.save(user); // сохраняем пользователя в базе данных

    }


    /**
     * Авторизация пользователя.
     *
     * @param request объект с данными пользователя для авторизации
     * @return объект с токеном авторизации
     */
    public AuthenticationResponseDto authenticate(LoginRequestDto request) {
        // Авторизация пользователя
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Поиск пользователя по имени пользователя
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String accessToken = jwtService.generateAccessToken(user); // генерируем токен авторизации
        String refreshToken = jwtService.generateRefreshToken(user); // генерируем токен обновления

        saveUserToken(refreshToken, user);

        // Возвращение объекта с токеном авторизации
        return new AuthenticationResponseDto(accessToken, refreshToken);
    }

    /**
     * Сохраняет токен авторизации пользователя в базе данных.
     *
     * @param refreshToken Токен обновления.
     * @param user Информация о пользователе.
     */
    private void saveUserToken(String refreshToken, User user) {
        // Создание объекта токена
        RefreshToken token = new RefreshToken();

        // Установка значения токена
        token.setToken(refreshToken);

        // Установка значения пользователя
        token.setUser(user);

        token.setExpiryDate(jwtService.calculateExpirationDate(refreshToken));

        // Сохранение токена в базе данных
        refreshTokenRepository.save(token);
    }

    /**
     * Обновляет токен аутентификации.
     *
     * @param request  HTTP-запрос.
     * @param response HTTP-ответ.
     * @return Ответ с обновленным токеном.
     */
    @Transactional
    public ResponseEntity<AuthenticationResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Получаем заголовок авторизации
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Проверяем наличие и формат токена
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Извлекаем токен из заголовка
        String token = authorizationHeader.substring(7);

        // Извлекаем имя пользователя из токена
        String email = jwtService.extractUsername(token);

        // Находим пользователя по имени
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));

        // Проверяем валидность токена обновления
        if (jwtService.isValidRefresh(token, user)) {

            // Генерируем новый доступный токен и обновляемый токен
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Заносим старый токен черный список
            BlackList blackList = new BlackList();
            blackList.setAccessToken(token);
            blackListRepository.save(blackList);

            try {
                // Удаляем старый токен из базы данных
                refreshTokenRepository.deleteByToken(token);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            // Сохраняем новый токен в базе данных
            saveUserToken(refreshToken, user);

            // Возвращаем новый ответ с токенами
            return new ResponseEntity<>(new AuthenticationResponseDto(accessToken, refreshToken), HttpStatus.OK);
        }

        // Удаляем старый токен из базы данных
        refreshTokenRepository.deleteByToken(token);

        // Возвращаем неавторизованный статус
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
