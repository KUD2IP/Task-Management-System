package org.example.authenticationservice.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.repository.BlackListRepository;
import org.example.authenticationservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret_key}")
    private String secretKey;

    @Value("${security.jwt.access_token_expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh_token_expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    private final BlackListRepository blackListRepository;

    public JwtService(RefreshTokenRepository refreshTokenRepository, BlackListRepository blackListRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.blackListRepository = blackListRepository;
    }


    public boolean isAccessValid(String token, UserDetails user) {
        // Извлекаем тип токена (например, в claim может быть дополнительный параметр для типа)
        String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));

        // Если это refresh токен, не пропускаем доступ к защищенному ресурсу
        if ("refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh token cannot access protected resource");
        }

        // Извлекаем имя пользователя из токена
        String username = extractUsername(token);

        // Проверяем, не истек ли токен и совпадает ли имя пользователя
        return username.equals(user.getUsername())
                && !isTokenExpired(token)
                && !isTokenInBlacklist(token);
    }

    private boolean isTokenInBlacklist(String token) {
        String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
        if ("refresh".equals(tokenType)) {
            return false;  // Refresh токены не должны проверяться в черном списке для доступа к защищенному ресурсу
        }
        return blackListRepository.existsByAccessToken(token);
    }

    public boolean isValidRefresh(String token, User user) {
        // Извлекаем тип токена (например, в claim может быть дополнительный параметр для типа)
        String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));

        // Если это refresh токен, не пропускаем доступ к защищенному ресурсу
        if ("access".equals(tokenType)) {
            throw new IllegalArgumentException("Access token cannot refresh tokens");
        }

        // Извлекаем имя пользователя из токена
        String username = extractUsername(token);

        if(refreshTokenRepository.findByToken(token).isEmpty()) {
            return false;
        }

        // Проверяем, не истек ли токен обновления и совпадает ли имя пользователя
        return username.equals(user.getUsername())
                && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            // Логируем ошибку, если не удается извлечь имя пользователя
            throw new IllegalArgumentException("Invalid token or username not found", e);
        }
    }


    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }


    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            // Логируем ошибку, если не удается извлечь дату истечения
            throw new IllegalArgumentException("Invalid token", e);
        }
    }


    private Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            // Логируем ошибку, если не удается извлечь дату истечения
            throw new IllegalArgumentException("Invalid token or expired", e);
        }
    }


    private Claims extractAllClaims(String token) {
        try {
            JwtParserBuilder parser = Jwts.parser();

            // Проверка подписи токена
            parser.verifyWith(getSignInKey());

            return parser.build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            // Логируем ошибку, если подпись токена неверна или сам токен некорректен
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }


    public String generateAccessToken(User user) {
        // Создание объекта JwtBuilder для создания токена
        return generateToken(user, accessTokenExpiration, "access");
    }


    public String generateRefreshToken(User user) {

        return generateToken(user, refreshTokenExpiration, "refresh");
    }


    private String generateToken(User user, long expiryTime, String tokenType) {
        JwtBuilder builder = Jwts.builder()
                // Установка субъекта токена (имя пользователя)
                .subject(user.getUsername())
                // Установка типа токена
                .claim("token_type", tokenType)
                // Установка времени выдачи токена (текущая дата)
                .issuedAt(new Date(System.currentTimeMillis()))
                // Установка времени истечения срока действия токена (текущая дата + 10 часов)
                .expiration(new Date(System.currentTimeMillis() + expiryTime))
                // Установка ключа для подписи токена
                .signWith(getSignInKey());

        // Создание и возврат токена в виде строки
        return builder.compact();
    }


    private SecretKey getSignInKey() {
        // Декодируем ключ из строки в массив байтов
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);

        // Возвращаем ключ для HmacSHA256
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long calculateExpirationDate(String refreshToken) {
        return extractExpiration(refreshToken).getTime() - new Date().getTime();
    }
}
