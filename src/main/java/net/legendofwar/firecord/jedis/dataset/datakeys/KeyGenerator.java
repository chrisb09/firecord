package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class KeyGenerator {

    public static Bytes getLockKey(Bytes key) {
        return DataKeyPrefix.LOCK.getBytes().append(key);
    }

    public static Bytes getPlayerKey(UUID uuid) {
        return DataKeyPrefix.PLAYER.getBytes().append(UUIDToByteArray(uuid));
    }

    public static Bytes getLookUpTableKey(Bytes key) {
        return DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append(key);
    }

    public static Bytes UUIDToByteArray(UUID uuid) {
        long l = uuid.getLeastSignificantBits();
        long m = uuid.getMostSignificantBits();
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(m);
        buffer.putLong(l);
        return new Bytes(buffer.array());
    }

}
