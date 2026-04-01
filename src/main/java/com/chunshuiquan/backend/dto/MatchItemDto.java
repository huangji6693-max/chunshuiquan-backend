package com.chunshuiquan.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class MatchItemDto {

    private UUID matchId;
    private OffsetDateTime createdAt;
    private boolean isNew;
    private OtherUserDto otherUser;
    private String lastMessage;              // 最后一条消息内容（截取50字符）
    private OffsetDateTime lastMessageAt;    // 最后消息时间
    private int unreadCount;                 // 未读消息数

    public MatchItemDto() {}

    public MatchItemDto(UUID matchId, OffsetDateTime createdAt, boolean isNew, OtherUserDto otherUser) {
        this.matchId = matchId;
        this.createdAt = createdAt;
        this.isNew = isNew;
        this.otherUser = otherUser;
    }

    public MatchItemDto(UUID matchId, OffsetDateTime createdAt, boolean isNew, OtherUserDto otherUser,
                        String lastMessage, OffsetDateTime lastMessageAt, int unreadCount) {
        this.matchId = matchId;
        this.createdAt = createdAt;
        this.isNew = isNew;
        this.otherUser = otherUser;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public OtherUserDto getOtherUser() { return otherUser; }
    public void setOtherUser(OtherUserDto otherUser) { this.otherUser = otherUser; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(OffsetDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public static class OtherUserDto {
        private UUID id;
        private String email;
        private String name;
        private String bio;
        private List<String> avatarUrls;
        private String jobTitle;
        private Integer height;
        private String education;
        private String zodiac;
        private String city;
        private String smoking;
        private String drinking;

        public OtherUserDto() {}

        public OtherUserDto(UUID id, String email, String name, String bio,
                            List<String> avatarUrls, String jobTitle,
                            Integer height, String education, String zodiac,
                            String city, String smoking, String drinking) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.bio = bio;
            this.avatarUrls = avatarUrls;
            this.jobTitle = jobTitle;
            this.height = height;
            this.education = education;
            this.zodiac = zodiac;
            this.city = city;
            this.smoking = smoking;
            this.drinking = drinking;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public List<String> getAvatarUrls() { return avatarUrls; }
        public void setAvatarUrls(List<String> avatarUrls) { this.avatarUrls = avatarUrls; }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

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
    }
}
