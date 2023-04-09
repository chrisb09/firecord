package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public abstract class KeyGenerator {

    public static byte[] getLockKey(byte[] key) {
        return join(DataKeyPrefix.LOCK.getData(), key);
    }

    public static byte[] getPlayerKey(UUID uuid) {
        return join(DataKeyPrefix.PLAYER.getData(), UUIDToByteArray(uuid));
    }

    public static byte[] getLookUpTableKey(byte[] key) {
        return join(DataKeyPrefix.KEY_LOOKUP_TABLE.getData(), key);
    }

    public static byte[] UUIDToByteArray(UUID uuid) {
        long l = uuid.getLeastSignificantBits();
        long m = uuid.getMostSignificantBits();
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES);
        buffer.putLong(m);
        buffer.putLong(l);
        return buffer.array();
    }

    public static byte[] bytes(byte b) {
        byte[] a = new byte[1];
        a[0] = b;
        return a;
    }

    public static byte[] join(byte a, byte b) {
        return join(bytes(a), bytes(b));
    }

    public static byte[] join(byte[] a, byte b) {
        return join(a, bytes(b));
    }

    public static byte[] join(byte a, byte[] b) {
        return join(bytes(a), b);
    }

    public static byte[] join(byte[]... parts) {
        int totalLength = Arrays.asList(parts).stream().mapToInt(part -> part.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        for (byte[] part : parts) {
            buffer.put(part);
        }
        return buffer.array();
    }

}
