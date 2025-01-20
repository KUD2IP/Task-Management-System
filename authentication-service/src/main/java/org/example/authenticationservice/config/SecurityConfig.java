package org.example.authenticationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.filter.JwtFilter;
import org.example.authenticationservice.handler.CustomAccessDeniedHandler;
import org.example.authenticationservice.handler.CustomLogoutHandler;
import org.example.authenticationservice.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@Slf4j
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

    /**
     * Настройка безопасности
     * Установка эндпоинтов, добавление обработчиков и конфигурации
     * Настройка фильтров
     *
     * @param http HttpSecurity
     * @return http.build() - объект HttpSecurity
     * @throws Exception если произошла ошибка
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Настраиваем авторизацию запросов
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/auth/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-resources/**",
                            "/actuator/**",
                            "swagger-ui.html").permitAll()    // Разрешаем все запросы к этим URL
                            .requestMatchers("/admin/**").hasAuthority("ADMIN")     // Разрешаем запросы только для администратора
                            .anyRequest().authenticated();      // Требуем аутентификацию для всех остальных запросов
                }).userDetailsService(userService)
                .exceptionHandling(e -> {
                    e.accessDeniedHandler(accessDeniedHandler) // Обработчик отказа доступа
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)); // Входная точка аутентификации
                })
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS)) // Управление сессиями
                .addFilterBefore(jwtFIlter, UsernamePasswordAuthenticationFilter.class) // Добавление фильтра JWT перед фильтром UsernamePasswordAuthenticationFilter
                .logout(log -> {
                    log.logoutUrl("/auth/logout")
                            .addLogoutHandler(customLogoutHandler) // Добавление обработчика выхода
                            .logoutSuccessHandler((request, response, authentication) ->
                                    SecurityContextHolder.clearContext()); // Очистка контекста безопасности после успешного выхода
                });
        return http.build();
    }
}
