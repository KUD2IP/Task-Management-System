package org.example.authenticationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.dto.UserResponseDto;
import org.example.authenticationservice.entity.Role;
import org.example.authenticationservice.entity.User;
import org.example.authenticationservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Метод для получения пользователя по его email.
     * @param email - email пользователя
     * @return пользователь
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));
    }


    /**
     * Метод для получения пользователя по его id.
     * @param id - id пользователя
     * @return пользователь
     */
    @Override
    public UserResponseDto getUserById(Long id) {
        //Получение пользователя по id
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //Формирование ответа
        UserResponseDto userResponseDto = new UserResponseDto();

        //Заполнение полей ответа
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setName(user.getName());
        userResponseDto.setRole(user.getRoles().stream().map(Role::getName).toList().toString());
        log.info(userResponseDto.toString());
        return userResponseDto;
    }
}
