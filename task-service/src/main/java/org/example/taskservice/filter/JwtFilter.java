package org.example.taskservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Value("${security.jwt.secret_key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        log.info("Received token: {}", token);
        if (StringUtils.hasText(token)) {
            Authentication authentication = getAuthenticationFromToken(token);

            log.info("Setting authentication: {}", authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Authentication getAuthenticationFromToken(String token) {
        JwtParserBuilder parser = Jwts.parser();

        log.info("Secret key: {}", secretKey);

        // Проверка подписи токена
        parser.verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)));

        log.info("Parsing token: {}", token);

        Claims claims = parser.build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        List<String> role = claims.get("roles", List.class);

        log.info("Found username: {}, role: {}", username, role.stream().toString());

        List<SimpleGrantedAuthority> authorities =  role.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }
}
