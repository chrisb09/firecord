package net.legendofwar.firecord.communication;

import net.legendofwar.firecord.jedis.dataset.ByteDataInterface;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public enum JedisCommunicationChannel implements ByteDataInterface {

    // @formatter:off
    SERVER_MESSAGE(1),                  // (node->node) message
    SERVER_MESSAGE_BROADCAST(2),        // (node->all other nodes) mssage
    TEST(3),                            // Test message, causes entry in log
    LOG(4),                             // Write to log of other node
    PING(5),                            // Ping another node
    PONG(6),                            // Answer to ping
    DEL_KEY(7),                         // signal a key is deleted
    UPDATE_SMALL_KEY(8),
    UPDATE_LARGE_KEY(9),
    DEL_KEY_VALUE(10),
    DEL_COLLECTION(11),
    LIST_ADD(12),
    LIST_ADD_ALL(13),
    LIST_ADD_INDEX(14),
    LIST_ADD_ALL_INDEX(15),
    LIST_REMOVE(16),
    LIST_REMOVE_INDEX(17),
    LIST_REMOVE_ALL(18),
    LIST_RETAIN_ALL(19),
    LIST_SET(20),
    LIST_LOG(21),                       // Debug print to compare redis and cache values

    CHAT_BROADCAST(100),
    
    ;

    // @formatter:on

    private Bytes data;

    private JedisCommunicationChannel(int id) {
        this.data = new Bytes(id, CHANNEL_BYTE_LENGTH);
    }

    public byte[] getData() {
        return data.getData();
    }

    public int getId() {
        return (int) data.decodeNumber();
    }

    public static final byte CHANNEL_BYTE_LENGTH = 2;

    @Override
    public Bytes getBytes() {
        return data;
    }

}
