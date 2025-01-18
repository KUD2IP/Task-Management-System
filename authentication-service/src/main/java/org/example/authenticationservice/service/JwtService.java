package org.example.authenticationservice.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.entity.Role;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    @Value("${security.jwt.secret_key}")
    private String secretKey;

    @Value("${security.jwt.access_token_expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh_token_expiration}")
    private long refreshTokenExpiration;

    private final TokenRepository tokenRepository;


    public JwtService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Проверяет валидность access токена.
     *
     * @param token Токен для проверки.
     * @param user Пользователь, для которого проверяется токен.
     * @return true, если токен валиден, иначе false.
     */
    public boolean isAccessValid(String token, UserDetails user) {
        log.debug("Checking if access token is valid for user: {}", user.getUsername());

        // Извлекаем тип токена из его "claims"
        String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
        if ("refresh".equals(tokenType)) {
            log.warn("Refresh token cannot access protected resource.");
            throw new IllegalArgumentException("Refresh token cannot access protected resource");
        }

        boolean isValidAccessToken = tokenRepository.findByAccessToken(token)
                .map(t -> !t.isLoggedOut()).orElse(false);

        // Проверка имени пользователя и валидности токена
        String username = extractUsername(token);
        boolean isValid = username.equals(user.getUsername())
                && !isTokenExpired(token)
                && isValidAccessToken;

        log.debug("Access token valid: {}", isValid);
        return isValid;
    }


    /**
     * Проверяет валидность refresh токена.
     *
     * @param token Токен для проверки.
     * @param user Пользователь, для которого проверяется токен.
     * @return true, если токен валиден, иначе false.
     */
    public boolean isValidRefresh(String token, User user) {
        log.debug("Checking if refresh token is valid for user: {}", user.getUsername());

        // Извлекаем тип токена
        String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
        if ("access".equals(tokenType)) {
            log.warn("Access token cannot refresh tokens.");
            throw new IllegalArgumentException("Access token cannot refresh tokens");
        }

        // Проверяем, есть ли в базе данных токен обновления с указанным значением
        boolean isValidRefreshToken = tokenRepository.findByRefreshToken(token)
                .map(t -> !t.isLoggedOut()).orElse(false);

        // Проверка имени пользователя и валидности refresh токена
        String username = extractUsername(token);

        boolean isValid = username.equals(user.getUsername())
                && !isTokenExpired(token)
                && isValidRefreshToken;

        log.debug("Refresh token valid: {}", isValid);
        return isValid;
    }

    /**
     * Извлекает имя пользователя из токена.
     *
     * @param token Токен, из которого извлекается имя пользователя.
     * @return Имя пользователя.
     * @throws IllegalArgumentException Если токен некорректен.
     */
    public String extractUsername(String token) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            log.debug("Extracted username: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Failed to extract username from token.", e);
            throw new IllegalArgumentException("Invalid token or username not found", e);
        }
    }

    /**
     * Извлекает данные из токена (claims).
     *
     * @param token Токен, из которого извлекаются данные.
     * @param resolver Функция для извлечения конкретного значения.
     * @param <T> Тип возвращаемого значения.
     * @return Извлеченные данные.
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Проверяет, истек ли срок действия токена.
     *
     * @param token Токен для проверки.
     * @return true, если токен истек, иначе false.
     */
    private boolean isTokenExpired(String token) {
        try {
            boolean expired = extractExpiration(token).before(new Date());
            log.debug("Token expired: {}", expired);
            return expired;
        } catch (Exception e) {
            log.error("Failed to extract expiration from token.", e);
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    /**
     * Извлекает дату истечения срока действия токена.
     *
     * @param token Токен для извлечения даты истечения.
     * @return Дата истечения.
     */
    private Date extractExpiration(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            log.debug("Extracted expiration date: {}", expiration);
            return expiration;
        } catch (Exception e) {
            log.error("Failed to extract expiration from token.", e);
            throw new IllegalArgumentException("Invalid token or expired", e);
        }
    }

    /**
     * Извлекает все claims из токена.
     *
     * @param token Токен для извлечения claims.
     * @return Все claims токена.
     */
    private Claims extractAllClaims(String token) {
        try {
            JwtParserBuilder parser = Jwts.parser();

            // Проверка подписи токена
            parser.verifyWith(getSignInKey());

            Claims claims = parser.build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("Extracted claims: {}", claims);
            return claims;
        } catch (JwtException e) {
            log.error("Invalid JWT token or signature.", e);
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Генерирует access токен для пользователя.
     *
     * @param user Пользователь для которого генерируется токен.
     * @return Сгенерированный токен.
     */
    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getUsername());
        return generateToken(user, accessTokenExpiration, "access");
    }

    /**
     * Генерирует refresh токен для пользователя.
     *
     * @param user Пользователь для которого генерируется токен.
     * @return Сгенерированный токен.
     */
    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getUsername());
        return generateToken(user, refreshTokenExpiration, "refresh");
    }

    /**
     * Генерирует JWT токен для пользователя с заданным временем истечения и типом токена.
     *
     * @param user Пользователь для которого генерируется токен.
     * @param expiryTime Время истечения токена.
     * @param tokenType Тип токена (access или refresh).
     * @return Сгенерированный токен.
     */
    private String generateToken(User user, long expiryTime, String tokenType) {
        JwtBuilder builder = Jwts.builder()
                .subject(user.getUsername())  // Установка email пользователя
                .claim("token_type", tokenType)  // Установка типа токена
                .claim("name", user.getName())  // Установка имени пользователя
                .issuedAt(new Date(System.currentTimeMillis()))  // Время выпуска токена
                .expiration(new Date(System.currentTimeMillis() + expiryTime))  // Время истечения токена
                .claims(Map.of(
                        "roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList())  // Добавление ролей в токен
                ))
                .signWith(getSignInKey());  // Подпись токена с использованием секретного ключа

        String token = builder.compact();
        log.debug("Generated {} token for user: {}", tokenType, user.getUsername());
        return token;
    }

    /**
     * Получение секретного ключа для подписи токенов.
     *
     * @return Секретный ключ для HmacSHA256.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Рассчитывает время истечения refresh токена.
     *
     * @param refreshToken Токен для расчета времени истечения.
     * @return Время истечения в миллисекундах.
     */
    public Long calculateExpirationDate(String refreshToken) {
        return extractExpiration(refreshToken).getTime() - new Date().getTime();
    }
}
