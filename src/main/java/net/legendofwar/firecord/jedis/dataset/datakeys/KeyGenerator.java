package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.util.UUID;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class KeyGenerator {

    private static KeyLookupTable dataGeneratorTable = new KeyLookupTable(DataKeyPrefix.DATA_GENERATOR.getBytes(), 2);
    private static KeyLookupTable playerUUIDtable = new KeyLookupTable(getDataGeneratorKey("playerUUIDs"), 4);

    public static Bytes getLockKey(Bytes key) {
        return DataKeyPrefix.LOCK.getBytes().append(key);
    }

    public static Bytes getPlayerKey(UUID uuid) {
        return DataKeyPrefix.PLAYER.getBytes().append(playerUUIDtable.lookUpId(new Bytes(uuid)));
    }

    public static Bytes getLookUpTableKey(Bytes key) {
        return DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append(key);
    }

    public static Bytes getDataGeneratorKey(String name){
        return DataKeyPrefix.DATA_GENERATOR.getBytes().append(dataGeneratorTable.lookUpId(name));
    }

}
