package org.example.authenticationservice.dto;

import lombok.Data;

@Data
public class RegistrationRequestDto {

    private String name;
    private String email;
    private String password;
}
