package org.example.safenest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}