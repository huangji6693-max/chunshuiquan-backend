package com.chunshuiquan.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank
    private String name;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotBlank
    @Pattern(regexp = "male|female|other")
    private String gender;
}
