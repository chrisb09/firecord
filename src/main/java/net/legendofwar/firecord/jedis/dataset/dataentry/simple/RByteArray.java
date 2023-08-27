package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public final class RByteArray extends LargeData<byte[]> {

    final static byte[] DEFAULT_VALUE = new byte[0];

    public RByteArray(@NotNull Bytes key) {
        this(key, null);
    }

    public RByteArray(@NotNull Bytes key, byte[] defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value);
    }

    @Override
    protected void fromBytes(@NotNull byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        get();
        return new Bytes(this.value).toString();
    }

}
