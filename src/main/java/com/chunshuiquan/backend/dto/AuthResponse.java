package com.chunshuiquan.backend.dto;

import com.chunshuiquan.backend.entity.Profile;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Data
public class AuthResponse {

    private String token;
    private String refreshToken;
    private UUID id;
    private String name;
    private String email;
    private int age;
    private String gender;
    private String jobTitle;
    private String bio;
    private String[] avatarUrls;
    private String[] tags;
    private Integer height;
    private String education;
    private String zodiac;
    private String city;
    private String smoking;
    private String drinking;
    private Double latitude;
    private Double longitude;
    private Boolean onboardingCompleted;
    private Integer coins;
    private String vipTier;
    private String birthDate;

    public static AuthResponse of(String token, String refreshToken, Profile p) {
        AuthResponse r = new AuthResponse();
        r.token = token;
        r.refreshToken = refreshToken;
        r.id = p.getId();
        r.name = p.getName();
        r.email = p.getEmail();
        r.age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
        r.gender = p.getGender();
        r.jobTitle = p.getJobTitle();
        r.bio = p.getBio();
        r.avatarUrls = p.getAvatarUrls();
        r.tags = p.getTags();
        r.height = p.getHeight();
        r.education = p.getEducation();
        r.zodiac = p.getZodiac();
        r.city = p.getCity();
        r.smoking = p.getSmoking();
        r.drinking = p.getDrinking();
        r.latitude = p.getLatitude();
        r.longitude = p.getLongitude();
        r.onboardingCompleted = p.getOnboardingCompleted();
        r.coins = p.getCoins();
        r.vipTier = p.getVipTier();
        r.birthDate = p.getBirthDate() != null ? p.getBirthDate().toString() : null;
        return r;
    }
}
