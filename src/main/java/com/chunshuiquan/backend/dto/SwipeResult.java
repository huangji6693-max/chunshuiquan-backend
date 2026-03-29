package com.chunshuiquan.backend.dto;

import java.util.UUID;

public class SwipeResult {
    private boolean matched;
    private UUID matchId;
    private String partnerName;
    private String partnerAvatarUrl;

    public SwipeResult(boolean matched, UUID matchId, String partnerName, String partnerAvatarUrl) {
        this.matched = matched;
        this.matchId = matchId;
        this.partnerName = partnerName;
        this.partnerAvatarUrl = partnerAvatarUrl;
    }

    public static SwipeResult noMatch() {
        return new SwipeResult(false, null, null, null);
    }

    public boolean isMatched() { return matched; }
    public UUID getMatchId() { return matchId; }
    public String getPartnerName() { return partnerName; }
    public String getPartnerAvatarUrl() { return partnerAvatarUrl; }
}
