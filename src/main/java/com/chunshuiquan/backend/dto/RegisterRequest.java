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

    private String name;          // 可选，默认用邮箱前缀

    private LocalDate birthDate;   // 可选，Onboarding中补填

    @NotBlank
    @Pattern(regexp = "male|female|other")
    private String gender;
}
