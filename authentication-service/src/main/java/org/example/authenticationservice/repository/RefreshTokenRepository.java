package org.example.authenticationservice.repository;

import org.example.authenticationservice.entity.RefreshToken;
import org.example.authenticationservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);
    void deleteByToken(String token);
}
