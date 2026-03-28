package com.chunshuiquan.backend.dto;

import com.chunshuiquan.backend.entity.Profile;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Data
public class AuthResponse {

    private String token;
    private UUID id;
    private String name;
    private String email;
    private int age;
    private String gender;
    private String jobTitle;
    private String bio;
    private String[] avatarUrls;
    private String[] tags;

    public static AuthResponse of(String token, Profile p) {
        AuthResponse r = new AuthResponse();
        r.token = token;
        r.id = p.getId();
        r.name = p.getName();
        r.email = p.getEmail();
        r.age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
        r.gender = p.getGender();
        r.jobTitle = p.getJobTitle();
        r.bio = p.getBio();
        r.avatarUrls = p.getAvatarUrls();
        r.tags = p.getTags();
        return r;
    }
}
