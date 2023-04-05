package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public final class RCharacter extends SmallData<Character> {

    final static Character DEFAULT_VALUE = ' ';

    public RCharacter(@NotNull String key) {
        this(key, null);
    }

    public RCharacter(@NotNull String key, Character defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        this.value = value.charAt(0);
    }

    @Override
    public String toString() {
        return "" + this.value;
    }

}
