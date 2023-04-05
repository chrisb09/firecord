package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public final class RByteArray extends LargeData<byte[]> {

    final static byte[] DEFAULT_VALUE = new byte[0];

    public RByteArray(@NotNull String key) {
        this(key, null);
    }

    public RByteArray(@NotNull String key, byte[] defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(@NotNull String value) {
        this.value = value.getBytes();
    }

    @Override
    public String toString() {
        return new String(this.value);
    }

}
