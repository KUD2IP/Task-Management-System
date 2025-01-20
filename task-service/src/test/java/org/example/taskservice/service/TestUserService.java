package org.example.taskservice.service;


import jakarta.servlet.http.HttpServletRequest;
import org.example.taskservice.entity.User;
import org.example.taskservice.exeception.UserAlreadyExistsException;
import org.example.taskservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestUserService {


    @Mock
    private HttpServletRequest request;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    public void testSaveUser_Success() {
        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        userService.saveUser(user);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testSaveUser_UserAlreadyExists() {

        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.saveUser(user));
    }

}
