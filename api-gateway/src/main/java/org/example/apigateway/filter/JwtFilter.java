package org.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.dto.TokenRequest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Фильтр для проверки JWT токенов на уровне API Gateway.
 * Этот фильтр проверяет наличие и валидность JWT токена в каждом запросе.
 * Если токен отсутствует или некорректен, возвращается статус 401 (UNAUTHORIZED).
 * В противном случае запрос передается дальше в цепочку фильтров.
 */
@Slf4j
@Component
public class JwtFilter implements GlobalFilter {

    private final WebClient.Builder webClientBuilder;

    public JwtFilter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Фильтр для проверки JWT токенов на уровне API Gateway с помощью Auth Service.
     *
     * @param exchange Объект Exchange, содержащий информацию о текущем запросе.
     * @param chain Следующий фильтр в цепочке.
     * @return Mono<Void> Объект Mono, содержащий результат фильтрации.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Логируем путь запроса
        log.debug("Request path: {}", path);

        // Пропускаем запросы на регистрацию и доступ к Swagger
        if (path.startsWith("/api/v1/auth/")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-resources")
                || path.contains("/actuator")
                || path.contains("/webjars")){
            log.debug("Public route detected, skipping token validation: {}", path);
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid (not a Bearer token)");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // Извлекаем токен

        log.debug("Received token: {}", token);

        // Проверка токена через Auth Service
        return isValidToken(token)
                .flatMap(valid -> { // Преобразование Mono<Boolean> в Mono<Void>
                    if (!valid) {
                        log.warn("Invalid token: {}", token);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED); // Если токен некорректен, возвращаем статус 401
                        return exchange.getResponse().setComplete();
                    }

                    log.debug("Token is valid: {}", token);
                    return chain.filter(exchange);
                });
    }

    /**
     * Проверяет валидность JWT токена.
     * Проверяет наличие и валидность токена в Auth Service.
     *
     * @param token JWT токен для проверки.
     * @return Mono<Boolean>, возвращающее true, если токен валиден, иначе false.
     */
    private Mono<Boolean> isValidToken(String token) {
        log.debug("Sending token to authentication service for validation: {}", token);

        return webClientBuilder.baseUrl("http://authentication-service:8081")
                .build()
                .post()
                .uri("api/v1/auth/validate-token")
                .bodyValue(new TokenRequest(token)) // Передаем токен в заголовке
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Error validating token: {}. Status code: {}", token, response.statusCode());
                    return Mono.empty(); // Если ошибка, возвращаем пустое Mono
                })
                .toBodilessEntity()  // Получаем только статусный код
                .map(response -> {
                    boolean isValid = response.getStatusCode().is2xxSuccessful(); // Проверяем код статуса, если 2xx, то токен валиден
                    log.debug("Token validation response: {}", isValid ? "valid" : "invalid");
                    return isValid;
                });
    }
}
