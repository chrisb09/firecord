package net.legendofwar.firecord.jedis.dataset.datakeys;

public enum DataKeySuffix {

    NONE(0x00),
    TYPE(0x01),
    CLASS(0x02),
    UPDATED(0x03);

    private byte data;

    private DataKeySuffix(int data) {
        this.data = (byte) data;
    }

    public byte getData() {
        return data;
    }

}
