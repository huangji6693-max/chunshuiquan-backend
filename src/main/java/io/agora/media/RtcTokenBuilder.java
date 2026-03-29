package io.agora.media;

/**
 * Convenience wrapper around AccessToken for RTC token generation.
 * Ported from Agora's open-source DynamicKey library.
 */
public class RtcTokenBuilder {

    public enum Role {
        /** Can publish audio/video streams */
        Role_Publisher(1),
        /** Subscribe-only */
        Role_Subscriber(2),
        /** Full admin privileges */
        Role_Admin(101);

        public final int initValue;

        Role(int i) {
            this.initValue = i;
        }
    }

    /**
     * Build a token for a numeric UID (pass 0 to let Agora assign one).
     */
    public String buildTokenWithUid(String appId, String appCertificate,
            String channelName, int uid, Role role, int privilegeExpiredTs) {
        return buildTokenWithAccount(appId, appCertificate, channelName,
                uid == 0 ? "" : String.valueOf(uid), role, privilegeExpiredTs);
    }

    /**
     * Build a token for a string account / user ID.
     */
    public String buildTokenWithAccount(String appId, String appCertificate,
            String channelName, String account, Role role, int privilegeExpiredTs) {
        AccessToken token = new AccessToken(appId, appCertificate, channelName, account);
        token.addPrivilege(AccessToken.Privileges.kJoinChannel, privilegeExpiredTs);
        if (role == Role.Role_Publisher || role == Role.Role_Admin) {
            token.addPrivilege(AccessToken.Privileges.kPublishAudioStream, privilegeExpiredTs);
            token.addPrivilege(AccessToken.Privileges.kPublishVideoStream, privilegeExpiredTs);
            token.addPrivilege(AccessToken.Privileges.kPublishDataStream, privilegeExpiredTs);
        }
        try {
            return token.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Agora token", e);
        }
    }
}
