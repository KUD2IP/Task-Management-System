package org.example.authenticationservice.config;

import org.example.authenticationservice.filter.JwtFilter;
import org.example.authenticationservice.handler.CustomAccessDeniedHandler;
import org.example.authenticationservice.handler.CustomLogoutHandler;
import org.example.authenticationservice.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFIlter;

    private final UserService userService;

    private final CustomAccessDeniedHandler accessDeniedHandler;

    private final CustomLogoutHandler customLogoutHandler;

    public SecurityConfig(JwtFilter jwtFIlter,
                          UserService userService,
                          CustomAccessDeniedHandler accessDeniedHandler, CustomLogoutHandler customLogoutHandler) {
        this.jwtFIlter = jwtFIlter;
        this.userService = userService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.customLogoutHandler = customLogoutHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Настраиваем авторизацию запросов
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/auth/**")
                            .permitAll()    // Разрешаем все запросы к этим URL
                            .requestMatchers("/admin/**").hasAuthority("ADMIN")     // Разрешаем запросы только для администратора
                            .anyRequest().authenticated();      // Требуем аутентификацию для всех остальных запросов
                }).userDetailsService(userService)
                .exceptionHandling(e -> {
                    e.accessDeniedHandler(accessDeniedHandler); // Обработчик отказа доступа
                    e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)); // Входная точка аутентификации
                })
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS)) // Управление сессиями
                .addFilterBefore(jwtFIlter, UsernamePasswordAuthenticationFilter.class) // Добавление фильтра JWT перед фильтром UsernamePasswordAuthenticationFilter
                .logout(log -> {
                    log.logoutUrl("/auth/logout");
                    log.addLogoutHandler(customLogoutHandler); // Добавление обработчика выхода
                    log.logoutSuccessHandler((request, response, authentication) ->
                            SecurityContextHolder.clearContext()); // Очистка контекста безопасности после успешного выхода
                });

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}