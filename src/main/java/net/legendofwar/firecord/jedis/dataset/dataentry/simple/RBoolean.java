package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;

public final class RBoolean extends SmallData<Boolean> {

    final static Boolean DEFAULT_VALUE = false;

    public RBoolean(@NotNull Bytes key) {
        this(key, null);
    }

    public RBoolean(@NotNull Bytes key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes(){
        return new Bytes(this.value ? 1 : 0, 1);
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = ByteFunctions.decodeNumber(value) != 0;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
