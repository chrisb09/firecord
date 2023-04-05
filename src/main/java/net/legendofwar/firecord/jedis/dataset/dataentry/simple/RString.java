package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public class RString extends SmallData<String> {

    final static String DEFAULT_VALUE = "";

    public RString(@NotNull String key) {
        this(key, null);
    }

    public RString(@NotNull String key, String defaultValue) {
        super(key, defaultValue);
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
