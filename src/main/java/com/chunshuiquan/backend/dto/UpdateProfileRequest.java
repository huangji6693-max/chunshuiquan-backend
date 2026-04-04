package com.chunshuiquan.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String name;

    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;

    @Size(max = 50, message = "职业不能超过50个字符")
    private String jobTitle;

    @Size(max = 50, message = "交友目的不能超过50个字符")
    private String lookingFor;

    @Min(value = 100, message = "身高不能低于100cm")
    @Max(value = 250, message = "身高不能超过250cm")
    private Integer height;        // 身高(cm)

    @Size(max = 30, message = "学历不能超过30个字符")
    private String education;      // 学历

    @Size(max = 20, message = "星座不能超过20个字符")
    private String zodiac;         // 星座

    @Size(max = 50, message = "城市不能超过50个字符")
    private String city;           // 所在城市

    @Size(max = 20, message = "吸烟选项不能超过20个字符")
    private String smoking;        // 吸烟

    @Size(max = 20, message = "饮酒选项不能超过20个字符")
    private String drinking;       // 饮酒

    @Min(value = -90, message = "纬度范围: -90 ~ 90")
    @Max(value = 90, message = "纬度范围: -90 ~ 90")
    private Double latitude;       // 纬度

    @Min(value = -180, message = "经度范围: -180 ~ 180")
    @Max(value = 180, message = "经度范围: -180 ~ 180")
    private Double longitude;      // 经度

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getLookingFor() { return lookingFor; }
    public void setLookingFor(String lookingFor) { this.lookingFor = lookingFor; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getZodiac() { return zodiac; }
    public void setZodiac(String zodiac) { this.zodiac = zodiac; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSmoking() { return smoking; }
    public void setSmoking(String smoking) { this.smoking = smoking; }
    public String getDrinking() { return drinking; }
    public void setDrinking(String drinking) { this.drinking = drinking; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
