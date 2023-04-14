package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public final class RCharacter extends SmallData<Character> {

    final static Character DEFAULT_VALUE = ' ';

    public RCharacter(@NotNull Bytes key) {
        this(key, null);
    }

    public RCharacter(@NotNull Bytes key, Character defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes((short) this.value.charValue(), Short.BYTES);
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = (char) (short) new Bytes(value).decodeNumber();
    }

    @Override
    public String toString() {
        return "" + this.value;
    }

}
