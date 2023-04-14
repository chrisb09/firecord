package net.legendofwar.firecord.jedis.dataset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * Bytes is a Wrapper around a byte[], that allows for .equals() to work, and
 * therefore implements hashCode in a way that relies solely on the values of
 * the entries
 */

public class Bytes implements ByteDataInterface {

    protected byte[] data;
    public int length;

    public Bytes() {
        set(new byte[0]);
    }

    public Bytes(byte[] data) {
        set(data);
    }

    public Bytes(String data) {
        set(data.getBytes());
    }

    public Bytes(ByteBuffer buffer) {
        buffer.position(0);
        set(buffer.array());
    }

    public Bytes(ByteDataInterface bdi) {
        set(bdi.getData());
    }

    public Bytes(byte b) {
        set(new byte[] { b });
    }

    public Bytes(int b) {
        this.encodeNumber(b);
    }

    public Bytes(long b) {
        this.encodeNumber(b);
    }

    public Bytes(long b, int bytes) {
        this.encodeNumber(b, bytes);
    }

    public Bytes(double b) {
        this.encodeDouble(b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass()))
            return false;
        Bytes bytesKey = (Bytes) o;
        return Arrays.equals(data, bytesKey.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    public Bytes append(Byte... append) {
        return this.append(Arrays.asList(append).stream().map(b -> new Bytes(b)).toArray(Bytes[]::new));
    }

    public Bytes append(ByteDataInterface... append) {
        return this.append(Arrays.asList(append).stream().map(bdi -> new Bytes(bdi)).toArray(Bytes[]::new));
    }

    public Bytes append(byte[]... append) {
        return this.append(Arrays.asList(append).stream().map(bytedata -> new Bytes(bytedata)).toArray(Bytes[]::new));
    }

    public Bytes append(Bytes... append) {
        byte[][] temp = new byte[append.length + 1][];
        temp[0] = this.data;
        for (int i = 0; i < append.length; i++) {
            temp[i + 1] = append[i].getData();
        }
        return new Bytes(join(temp));
    }

    public void set(byte[] data) {
        this.data = data;
        length = data.length;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public Bytes getBytes() {
        return this;
    }

    @Override
    public String toString() {
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

    public String asString() {
        return new String(data);
    }

    /**
     * @formatter:off
     * @param l     The long which we want to encode
     * @param bytes How many bytes are we going to use to encode
     * @return      <bytes> sized byte[] containing data from l
     * @formatter:on
     */
    public Bytes encodeNumber(long l, int bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(l);
        buffer.position(0);
        byte[] result = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            result[i] = buffer.get();
        }
        set(result);
        return this;
    }

    public Bytes encodeNumber(long l) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(l);
        buffer.position(0);
        this.set(buffer.array());
        return this;
    }

    public Bytes encodeNumber(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(i);
        buffer.position(0);
        this.set(buffer.array());
        return this;
    }

    public long decodeNumber() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(Long.BYTES - data.length);
        buffer.put(data);
        if (data[0] >> 7 != 0) { // negative number
            for (int i = 0; i < Long.BYTES - data.length; i++) {
                // fill remaining bytes with 0xFF for proper negative numbers
                // (not required for positive numbers as 0s are default)
                buffer.put((byte) 0b11111111);
            }
        }
        buffer.position(0);
        return buffer.getLong();
    }

    public Bytes encodeDouble(double d) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(d);
        set(buffer.array());
        return this;
    }

    public double decodeDouble() {
        if (data.length != Double.BYTES) {
            throw new InvalidParameterException(
                    "We can only read a double if and only if the data byte[] has " + Double.BYTES + " entries.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(this.data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
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
