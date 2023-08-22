package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public final class RString extends SmallData<String> {

    final static String DEFAULT_VALUE = "";

    public RString(@NotNull Bytes key) {
        this(key, null);
    }

    public RString(@NotNull Bytes key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value);
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = new String(value);
    }

    @Override
    public String toString() {
        return this.value;
    }

}
