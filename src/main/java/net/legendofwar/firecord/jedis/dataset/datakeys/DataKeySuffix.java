package net.legendofwar.firecord.jedis.dataset.datakeys;

import net.legendofwar.firecord.jedis.dataset.ByteDataInterface;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public enum DataKeySuffix implements ByteDataInterface {

    // @formatter:off
    SPECIFIC(0x00),         // Used for unique entries like id-counter for DataGenerator
    TYPE(0x01),             // Specifies the type of entry
    CLASS(0x02),            // Specifies the class, used for OBJECTs
    UPDATED(0x03),          // Specifies the last time this entry was cared for by a DataGenerator
    MODIFIER(0x04);         // Specifies modifiers such as if it was created by a DataGenerator (1)

    // @formatter:on

    private Bytes data;

    private DataKeySuffix(int data) {
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
