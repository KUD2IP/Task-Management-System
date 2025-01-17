package org.example.authenticationservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.dto.AuthenticationResponseDto;
import org.example.authenticationservice.dto.LoginRequestDto;
import org.example.authenticationservice.dto.RegistrationRequestDto;
import org.example.authenticationservice.dto.TokenRequest;
import org.example.authenticationservice.service.AuthenticationService;
import org.example.authenticationservice.service.JwtService;
import org.example.authenticationservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(AuthenticationService authenticationService,
                          UserService userService,
                          JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.jwtService = jwtService;
    }


    /**
     * Регистрация нового пользователя.
     *
     * @param registrationDto данные для регистрации
     * @return ответ о результате регистрации
     */
    @PostMapping("/registration")
    public ResponseEntity<?> register(
            @RequestBody RegistrationRequestDto registrationDto
    ) {
        log.info("Registration request: {}", registrationDto);
        authenticationService.register(registrationDto);

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDto> authenticate(
            @RequestBody LoginRequestDto request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/refresh_token")
    public ResponseEntity<AuthenticationResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return authenticationService.refreshToken(request, response);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestBody TokenRequest tokenRequest) {

        String username = jwtService.extractUsername(tokenRequest.getToken());

        if (username == null) {
            return ResponseEntity.ok(false);
        }

        UserDetails userDetails = userService.loadUserByUsername(username);

        return ResponseEntity.ok(jwtService.isAccessValid(tokenRequest.getToken(), userDetails));
    }
}