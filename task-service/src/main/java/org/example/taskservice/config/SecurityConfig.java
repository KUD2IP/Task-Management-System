package org.example.taskservice.config;

import org.example.taskservice.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityWebFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests((requests) ->
                    requests.requestMatchers(
                                    "/tasks/v3/api-docs",
                                    "/tasks/v3/api-docs/**",
                                    "/tasks/swagger-ui/**",
                                    "/tasks/swagger-resources/**",
                                    "/tasks/actuator/**",
                                    "/tasks/swagger-ui.html").permitAll()
                            .requestMatchers("/tasks/status/**").hasAnyRole("ADMIN", "USER_EXECUTOR")
                            .requestMatchers("/tasks/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
            ).sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
