package org.example.authenticationservice.repository;

import org.example.authenticationservice.entity.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    Optional<BlackList> findByAccessToken(String accessToken);
    boolean existsByAccessToken(String accessToken);
}
