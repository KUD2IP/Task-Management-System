package org.example.authenticationservice.service;

import org.example.authenticationservice.dto.UserResponseDto;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public interface UserService extends UserDetailsService {
    UserResponseDto getUserById(Long id);
}
