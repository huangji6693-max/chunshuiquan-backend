package com.chunshuiquan.backend.util.agora;

/**
 * Builds Agora RTC tokens using the AccessToken2 (007) format.
 */
public class RtcTokenBuilder2 {

    public enum Role {
        ROLE_PUBLISHER(1),
        ROLE_SUBSCRIBER(2);

        private final int value;
        Role(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    /**
     * @param appId                Agora app ID
     * @param appCertificate       Agora app certificate
     * @param channelName          Channel name (e.g. matchId)
     * @param uid                  User ID; pass 0 for Agora auto-assign
     * @param role                 ROLE_PUBLISHER or ROLE_SUBSCRIBER
     * @param tokenExpireSeconds   How long the token itself is valid
     * @param privilegeExpireSeconds  How long channel privileges are valid
     */
    public String buildTokenWithUid(String appId, String appCertificate, String channelName,
                                    int uid, Role role,
                                    int tokenExpireSeconds, int privilegeExpireSeconds) throws Exception {
        String uidStr = (uid == 0) ? "" : String.valueOf(uid);
        return buildTokenWithUserAccount(appId, appCertificate, channelName, uidStr,
                role, tokenExpireSeconds, privilegeExpireSeconds);
    }

    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName,
                                            String account, Role role,
                                            int tokenExpireSeconds, int privilegeExpireSeconds) throws Exception {
        AccessToken2 token = new AccessToken2(appId, appCertificate, tokenExpireSeconds);
        AccessToken2.ServiceRtc serviceRtc = new AccessToken2.ServiceRtc(channelName, account);

        int expireTs = (int) (System.currentTimeMillis() / 1000) + privilegeExpireSeconds;
        serviceRtc.addPrivilege(AccessToken2.PRIVILEGE_JOIN_CHANNEL, expireTs);

        if (role == Role.ROLE_PUBLISHER) {
            serviceRtc.addPrivilege(AccessToken2.PRIVILEGE_PUBLISH_AUDIO_STREAM, expireTs);
            serviceRtc.addPrivilege(AccessToken2.PRIVILEGE_PUBLISH_VIDEO_STREAM, expireTs);
            serviceRtc.addPrivilege(AccessToken2.PRIVILEGE_PUBLISH_DATA_STREAM, expireTs);
        }

        token.addServiceRtc(serviceRtc);
        return token.build();
    }
}
