package org.example.taskservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.entity.User;
import org.example.taskservice.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final UserService userService;

    public JwtFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = getAuthenticationFromToken(request);

        log.info("Setting authentication: {}", authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private Authentication getAuthenticationFromToken(HttpServletRequest request) throws IOException {

        User user = userService.getClaimsFromToken(request);

        if (user == null) {
            return null;
        }
        String username = user.getEmail();
        List<String> role = List.of(user.getRole().split(","));

        List<SimpleGrantedAuthority> authorities =  role.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        log.info("authorities: {}", authorities);
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }
}
