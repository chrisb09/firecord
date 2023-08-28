package net.legendofwar.firecord.jedis.dataset.datakeys;

import net.legendofwar.firecord.jedis.dataset.ByteDataInterface;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public enum DataKeyPrefix implements ByteDataInterface {

    // @formatter:off
    NODE(0),                    // For saving node-related organizational data
    SERVER(1),
    TEAM(2),                    // For saving volk-related data
    CITY(3),                    // For saving city-related data
    GROUP(4),
    PLAYER(5),                  // For saving player-related data
    AUCTION(6),                 // For saving auction-related data
    CLAIM(7),                   // For saving claim-related data
    LOCK(64),                   // For saving lock-related data
    KEY_LOOKUP_TABLE(65),       // For saving lookuptable-related data
    DATA_GENERATOR(66),         // For saving datagenerator-related data
    ;
    // @formatter:on

    private Bytes data;

    private DataKeyPrefix(int data) {
        this.data = new Bytes((byte) data);
    }

    public byte[] getData() {
        return data.getData();
    }

    @Override
    public Bytes getBytes() {
        return data;
    }

}
