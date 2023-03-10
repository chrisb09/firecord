package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public class RBoolean extends SmallData<Boolean> {

    public RBoolean(String key, @NotNull Boolean defaultValue) {
        super(key, defaultValue, DataType.BOOLEAN);
    }

    public RBoolean(String key) {
        this(key, false);
    }

    @Override
    protected void fromString(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
