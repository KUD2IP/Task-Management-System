package org.example.authenticationservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authenticationservice.dto.AuthenticationResponseDto;
import org.example.authenticationservice.dto.LoginRequestDto;
import org.example.authenticationservice.dto.RegistrationRequestDto;
import org.example.authenticationservice.entity.Role;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.exception.UserNotFoundException;
import org.example.authenticationservice.repository.RoleRepository;
import org.example.authenticationservice.repository.TokenRepository;
import org.example.authenticationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class TestServiceAuthentication {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPassword("password123");
        user.setRoles(new HashSet<>(List.of(Role.builder().id(1L).name("ROLE_USER").build())));
    }

    @Test
    public void testRegisterUserAlreadyExists() {
        RegistrationRequestDto request = RegistrationRequestDto.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Exception exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals("User with email test@example.com already exists", exception.getMessage());
    }

    @Test
    public void testRegisterUserSuccessfully() {
        RegistrationRequestDto request = RegistrationRequestDto.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(Role.builder().id(1L).name("ROLE_USER").build()));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        authenticationService.register(request);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testAuthenticateUserNotFound() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.authenticate(request);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testAuthenticateUserSuccessfully() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");

        AuthenticationResponseDto response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    public void testRefreshTokenSuccessfully() {
        when(jwtService.extractUsername("refreshToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.isValidRefresh("refreshToken", user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("newRefreshToken");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer refreshToken");

        ResponseEntity<AuthenticationResponseDto> result = authenticationService.refreshToken(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("newAccessToken", result.getBody().getAccessToken());
        assertEquals("newRefreshToken", result.getBody().getRefreshToken());
    }

    @Test
    public void testAssignRoleToUserSuccessfully() {
        Long userId = 1L;
        Role role = Role.builder().id(1L).name("ROLE_EXECUTOR").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_EXECUTOR")).thenReturn(Optional.of(role));
        when(userRepository.save(user)).thenReturn(user);

        authenticationService.assignRoleToUser(userId);

        assertTrue(user.getRoles().contains(role));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testAssignRoleToUserUserNotFound() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(UserNotFoundException.class, () -> {
            authenticationService.assignRoleToUser(userId);
        });

        assertEquals("User not found", exception.getMessage());
    }
}