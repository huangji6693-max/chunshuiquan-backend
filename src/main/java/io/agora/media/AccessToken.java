package io.agora.media;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.zip.CRC32;

/**
 * Agora AccessToken builder — ported from
 * https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/java
 */
public class AccessToken {

    static final String VERSION = "006";

    public String appID = "";
    public byte[] appCertificate;
    public String channelName = "";
    public String uid = "";
    public int ts;
    public int salt;
    public TreeMap<Short, Integer> messages = new TreeMap<>();

    public enum Privileges {
        kJoinChannel((short) 1),
        kPublishAudioStream((short) 2),
        kPublishVideoStream((short) 3),
        kPublishDataStream((short) 4);

        public final short intValue;

        Privileges(short i) {
            this.intValue = i;
        }
    }

    public AccessToken(String appID, String appCertificate, String channelName, String uid) {
        this.appID = appID;
        this.appCertificate = appCertificate.getBytes();
        this.channelName = channelName;
        this.uid = uid;
        this.ts = (int) (System.currentTimeMillis() / 1000);
        this.salt = new Random().nextInt(Integer.MAX_VALUE) + 1;
    }

    public void addPrivilege(Privileges privilege, int expireTimestamp) {
        this.messages.put(privilege.intValue, expireTimestamp);
    }

    public String build() throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        byte[] signing = generateSigning();
        byte[] msgBuf = pack(signing);

        CRC32 crc = new CRC32();
        crc.update(channelName.getBytes());
        int crcChannel = (int) crc.getValue();

        crc.reset();
        crc.update(uid.getBytes());
        int crcUid = (int) crc.getValue();

        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + msgBuf.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(crcChannel);
        buf.putInt(crcUid);
        buf.put(msgBuf);

        return VERSION + appID + Base64.getEncoder().encodeToString(buf.array());
    }

    private byte[] generateSigning() throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(appID.getBytes());

        ByteBuffer tsBuf = ByteBuffer.allocate(4);
        tsBuf.order(ByteOrder.LITTLE_ENDIAN);
        tsBuf.putInt(ts);
        stream.write(tsBuf.array());

        ByteBuffer saltBuf = ByteBuffer.allocate(4);
        saltBuf.order(ByteOrder.LITTLE_ENDIAN);
        saltBuf.putInt(salt);
        stream.write(saltBuf.array());

        stream.write(channelName.getBytes());
        stream.write(uid.getBytes());

        for (Map.Entry<Short, Integer> entry : messages.entrySet()) {
            ByteBuffer b = ByteBuffer.allocate(2 + 4);
            b.order(ByteOrder.LITTLE_ENDIAN);
            b.putShort(entry.getKey());
            b.putInt(entry.getValue());
            stream.write(b.array());
        }

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(appCertificate, "HmacSHA256"));
        return hmac.doFinal(stream.toByteArray());
    }

    private byte[] pack(byte[] signing) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        ByteBuffer header = ByteBuffer.allocate(4 + 4 + 2);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(salt);
        header.putInt(ts);
        header.putShort((short) messages.size());
        stream.write(header.array());

        for (Map.Entry<Short, Integer> entry : messages.entrySet()) {
            ByteBuffer b = ByteBuffer.allocate(2 + 4);
            b.order(ByteOrder.LITTLE_ENDIAN);
            b.putShort(entry.getKey());
            b.putInt(entry.getValue());
            stream.write(b.array());
        }

        ByteBuffer sigBuf = ByteBuffer.allocate(2 + signing.length);
        sigBuf.order(ByteOrder.LITTLE_ENDIAN);
        sigBuf.putShort((short) signing.length);
        sigBuf.put(signing);
        stream.write(sigBuf.array());

        return stream.toByteArray();
    }
}
