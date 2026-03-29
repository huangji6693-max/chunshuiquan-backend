package com.chunshuiquan.backend.util.agora;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;

/**
 * Agora AccessToken2 builder (007 token format).
 * Ported from AgoraIO/Tools DynamicKey Java implementation (Apache 2.0).
 */
public class AccessToken2 {

    public static final String VERSION = "007";

    // RTC privileges
    public static final short PRIVILEGE_JOIN_CHANNEL = 1;
    public static final short PRIVILEGE_PUBLISH_AUDIO_STREAM = 2;
    public static final short PRIVILEGE_PUBLISH_VIDEO_STREAM = 3;
    public static final short PRIVILEGE_PUBLISH_DATA_STREAM = 4;

    private final String appId;
    private final String appCertificate;
    private final int expire;
    private int issueTs;
    private int salt;
    private final TreeMap<Short, ServiceRtc> services = new TreeMap<>();

    public AccessToken2(String appId, String appCertificate, int expire) {
        this.appId = appId;
        this.appCertificate = appCertificate;
        this.expire = expire;
        this.issueTs = (int) (System.currentTimeMillis() / 1000);
        this.salt = new Random().nextInt(99999999) + 1;
    }

    public void addServiceRtc(ServiceRtc service) {
        services.put(ServiceRtc.SERVICE_TYPE, service);
    }

    public String build() throws Exception {
        // 1. Pack services
        ByteBuffer bufServices = ByteBuffer.allocate(512);
        bufServices.order(ByteOrder.LITTLE_ENDIAN);
        bufServices.putShort((short) services.size());
        for (ServiceRtc svc : services.values()) {
            svc.pack(bufServices);
        }
        int servicesLen = bufServices.position();
        byte[] servicesBytes = new byte[servicesLen];
        System.arraycopy(bufServices.array(), 0, servicesBytes, 0, servicesLen);

        // 2. Signing key = HMAC-SHA256(appCertificate, LE(issueTs) || LE(salt))
        byte[] tsAndSalt = ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(issueTs)
                .putInt(salt)
                .array();
        byte[] signingKey = hmacSha256(appCertificate.getBytes(StandardCharsets.UTF_8), tsAndSalt);

        // 3. Signing content = appId_bytes || str(issueTs)_bytes || str(salt)_bytes || str(expire)_bytes || servicesBytes
        byte[] appIdBytes = appId.getBytes(StandardCharsets.UTF_8);
        byte[] issueTsBytes = String.valueOf(issueTs).getBytes(StandardCharsets.UTF_8);
        byte[] saltBytes = String.valueOf(salt).getBytes(StandardCharsets.UTF_8);
        byte[] expireBytes = String.valueOf(expire).getBytes(StandardCharsets.UTF_8);
        byte[] signingContent = concat(appIdBytes, issueTsBytes, saltBytes, expireBytes, servicesBytes);

        // 4. Signature = HMAC-SHA256(signingKey, signingContent)
        byte[] signature = hmacSha256(signingKey, signingContent);

        // 5. Pack full token: packString(appId) + uint32(issueTs) + uint32(expire) + uint32(salt) + packBytes(signature) + services
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        packString(buf, appId);
        buf.putInt(issueTs);
        buf.putInt(expire);
        buf.putInt(salt);
        packBytes(buf, signature);
        buf.put(servicesBytes);

        // 6. Compress (zlib) + Base64
        byte[] raw = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, raw, 0, buf.position());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(raw);
        }

        return VERSION + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // ---------- helpers ----------

    static void packString(ByteBuffer buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf.putShort((short) bytes.length);
        buf.put(bytes);
    }

    static void packBytes(ByteBuffer buf, byte[] bytes) {
        buf.putShort((short) bytes.length);
        buf.put(bytes);
    }

    static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }

    // ---------- inner class ----------

    public static class ServiceRtc {
        public static final short SERVICE_TYPE = 1;

        private final String channelName;
        private final String uid;
        private final TreeMap<Short, Integer> privileges = new TreeMap<>();

        public ServiceRtc(String channelName, String uid) {
            this.channelName = channelName;
            this.uid = uid;
        }

        public void addPrivilege(short privilege, int expireTs) {
            privileges.put(privilege, expireTs);
        }

        void pack(ByteBuffer buf) {
            buf.putShort(SERVICE_TYPE);
            AccessToken2.packString(buf, channelName);
            AccessToken2.packString(buf, uid);
            buf.putShort((short) privileges.size());
            for (Map.Entry<Short, Integer> e : privileges.entrySet()) {
                buf.putShort(e.getKey());
                buf.putInt(e.getValue());
            }
        }
    }
}
