package org.example.authenticationservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.service.JwtService;
import org.example.authenticationservice.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    private final UserService userService;

    public JwtFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Получаем заголовок Authorization
        String authHeader = request.getHeader("Authorization");

        // Если заголовок не содержит JWT-токена, пропускаем фильтр
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("Request received: {}", request.getRequestURI());
            log.info("Security context: {}", SecurityContextHolder.getContext().getAuthentication());
            filterChain.doFilter(request, response);
            return;
        }


        // Извлекаем JWT-токен из заголовка
        String token = authHeader.substring(7);

        log.info("Extracted token: {}", token);

        if (jwtService.extractClaim(token, claims -> "refresh".equals(claims.get("token_type", String.class)))) {
            log.info("Request received: {}", request.getRequestURI());
            log.info("Security context: {}", SecurityContextHolder.getContext().getAuthentication());
            filterChain.doFilter(request, response);
            return;
        }
        // Извлекаем имя пользователя из JWT-токена
        String username = jwtService.extractUsername(token);

        // Если имя пользователя не пустое и аутентификация не установлена,
        // проверяем валидность токена и устанавливаем аутентификацию пользователя
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            log.info("Extracted username: {}", username);
            // Загружаем детали пользователя
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Проверяем валидность токена для данного пользователя
            if(jwtService.isAccessValid(token, userDetails)) {
                // Создаем объект аутентификации с деталями пользователя
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Устанавливаем детали аутентификации
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                log.info("Setting authentication: {}", authToken);

                // Устанавливаем аутентификацию
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        log.info("Request received: {}", request.getRequestURI());
        log.info("Security context: {}", SecurityContextHolder.getContext().getAuthentication());
        // Пропускаем фильтр
        filterChain.doFilter(request, response);

    }
}
