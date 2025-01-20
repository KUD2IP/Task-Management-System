package org.example.authenticationservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.dto.*;
import org.example.authenticationservice.service.AuthenticationService;
import org.example.authenticationservice.service.JwtService;
import org.example.authenticationservice.service.UserService;
import org.example.authenticationservice.service.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserServiceImpl userServiceImpl;

    public AuthController(AuthenticationService authenticationService,
                          UserService userService,
                          JwtService jwtService, UserServiceImpl userServiceImpl) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.userServiceImpl = userServiceImpl;
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param registrationDto данные для регистрации
     * @return ответ о результате регистрации
     */
    @PostMapping("/registration")
    public ResponseEntity<?> register(@RequestBody RegistrationRequestDto registrationDto) {
        log.info("Registration request: {}", registrationDto);
        try {
            authenticationService.register(registrationDto);
            return ResponseEntity.accepted().build(); // Возвращаем статус 202 Accepted
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Аутентификация пользователя.
     *
     * @param request данные для входа
     * @return токен доступа
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDto> authenticate(@RequestBody LoginRequestDto request) {
        log.info("Login request: {}", request.getEmail());
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request));
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }

    /**
     * Обновление токена доступа.
     *
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @return новый токен доступа
     */
    @PostMapping("/refresh_token")
    public ResponseEntity<AuthenticationResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Refresh token request");
        try {
            return authenticationService.refreshToken(request, response);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
    }

    /**
     * Валидация токена доступа.
     *
     * @param tokenRequest запрос с токеном
     * @return true, если токен валиден, иначе false
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestBody TokenRequest tokenRequest) {
        log.info("Validate token request");
        try {
            String username = jwtService.extractUsername(tokenRequest.getToken());

            if (username == null) {
                log.warn("Token validation failed: username not found");
                return ResponseEntity.ok(false);
            }

            UserDetails userDetails = userService.loadUserByUsername(username);
            boolean isValid = jwtService.isAccessValid(tokenRequest.getToken(), userDetails);
            log.info("Token is valid: {}", isValid);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Выдача роли исполнителю.
     *
     * @param userId идентификатор пользователя
     * @return ответ о результате выдачи роли
     */
    @PostMapping("/executor/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignExecutor(
            @PathVariable Long userId) {
        authenticationService.assignRoleToUser(userId);
        return ResponseEntity.ok("Executor assigned successfully");
    }

    /**
     * Поиск пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return пользователь
     */
    @GetMapping("/executor/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getExecutor(
            @PathVariable Long userId) {
        return ResponseEntity.ok(userServiceImpl.getUserById(userId));
    }
}
