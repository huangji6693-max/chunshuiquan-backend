package com.chunshuiquan.backend.dto;

public class UpdateProfileRequest {
    private String name;
    private String bio;
    private String jobTitle;
    private String lookingFor;
    private Integer height;        // 身高(cm)
    private String education;      // 学历
    private String zodiac;         // 星座
    private String city;           // 所在城市
    private String smoking;        // 吸烟
    private String drinking;       // 饮酒
    private Double latitude;       // 纬度
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
