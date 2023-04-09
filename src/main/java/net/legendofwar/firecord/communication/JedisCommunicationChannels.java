package net.legendofwar.firecord.communication;

import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;

public enum JedisCommunicationChannels {

    SERVER_MESSAGE(1),
    SERVER_MESSAGE_BROADCAST(2),
    TEST(3),
    LOG(4),
    PING(5),
    PONG(6);

    private byte[] data;

    private JedisCommunicationChannels(int id) {
        this.data = ByteFunctions.encodeId(id, 2);
    }

    public byte[] getData() {
        return data;
    }

    public int getId() {
        return (int) ByteFunctions.decodeId(data);
    }

}
