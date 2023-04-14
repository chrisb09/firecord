package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import net.legendofwar.firecord.jedis.dataset.ByteDataInterface;

public class ByteFunctions {

    /**
     * @formatter:off
     * @param l     The long which we want to encode
     * @param bytes How many bytes are we going to use to encode
     * @return      <bytes> sized byte[] containing data from l
     * @formatter:on
     */
    public static byte[] encodeNumber(long l, int bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(l);
        buffer.position(0);
        byte[] result = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            result[i] = buffer.get();
        }
        return result;
    }

    public static byte[] encodeNumber(long l) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(l);
        buffer.position(0);
        return buffer.array();
    }

    public static long decodeNumber(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(Long.BYTES - data.length);
        buffer.put(data);
        buffer.position(0);
        return buffer.getLong();
    }

    public static byte[] encodeDouble(double d) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(d);
        return buffer.array();
    }

    public static double decodeDouble(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(Double.BYTES - data.length);
        buffer.put(data);
        buffer.position(0);
        return buffer.getDouble();
    }

    public static final String asHexadecimal(byte[] data) {
        if (data == null) {
            return null;
        }
        char[] hexDigits = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byte num = data[i];
            hexDigits[i * 2 + 0] = Character.forDigit((num >> 4) & 0xF, 16);
            hexDigits[i * 2 + 1] = Character.forDigit((num & 0xF), 16);
        }
        return "0x" + new String(hexDigits);
    }

    public static byte[] bytes(byte b) {
        byte[] a = new byte[1];
        a[0] = b;
        return a;
    }

    public static byte[] bytes(ByteDataInterface b) {
        return b.getData();
    }

    public static byte[] bytes(byte[] b) {
        return b;
    }

    public static byte[] join(byte a, byte b) {
        return join(bytes(a), bytes(b));
    }

    public static byte[] join(byte[] a, byte b) {
        return join(a, bytes(b));
    }

    public static byte[] join(ByteDataInterface a, byte b) {
        return join(a.getData(), bytes(b));
    }

    public static byte[] join(byte a, ByteDataInterface b) {
        return join(bytes(a), b.getData());
    }

    public static byte[] join(byte[] a, ByteDataInterface b) {
        return join(a, b.getData());
    }

    public static byte[] join(ByteDataInterface a, byte[] b) {
        return join(a.getData(), b);
    }

    public static byte[] join(ByteDataInterface a, ByteDataInterface b) {
        return join(a.getData(), b.getData());
    }

    public static byte[] join(byte[]... parts) {
        int totalLength = Arrays.asList(parts).stream().mapToInt(part -> part.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (byte[] part : parts) {
            buffer.put(part);
        }
        buffer.position(0);
        return buffer.array();
    }

}
