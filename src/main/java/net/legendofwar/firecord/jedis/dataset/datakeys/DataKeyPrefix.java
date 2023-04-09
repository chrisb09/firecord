package net.legendofwar.firecord.jedis.dataset.datakeys;

public enum DataKeyPrefix {

    PLAYER(1),
    VOLK(2),
    CITY(3),
    AUCTION(4),
    LOCK(64),
    KEY_LOOKUP_TABLE(65);

    private byte data;

    private DataKeyPrefix(int data) {
        this.data = (byte) data;
    }

    public byte getData() {
        return data;
    }

}
