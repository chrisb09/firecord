package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.nio.ByteBuffer;

public class ByteFunctions {

    /**
     * @formatter:off
     * @param l     The long which we want to encode
     * @param bytes How many bytes are we going to use to encode
     * @return      <bytes> sized byte[] containing data from l
     * @formatter:on
     */
    public static byte[] encodeId(long l, int bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(l);
        buffer.position(Long.BYTES - bytes);
        return buffer.slice().array();
    }

    public static long decodeId(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.position(Long.BYTES - data.length);
        buffer.put(data);
        buffer.position(0);
        return buffer.getLong();
    }

    public static final String asHexadecimal(byte[] data) {
        if (data == null) {
            return null;
        }
        char[] hexDigits = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byte num = data[i];
            hexDigits[i + 0] = Character.forDigit((num >> 4) & 0xF, 16);
            hexDigits[i + 1] = Character.forDigit((num & 0xF), 16);
        }
        return new String(hexDigits);
    }

}
