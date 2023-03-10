package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public class RString extends SmallData<String> {

    public RString(String key, @NotNull String defaultValue) {
        super(key, defaultValue, DataType.STRING);
    }

    public RString(String key) {
        this(key, "");
    }

    @Override
    protected void fromString(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
