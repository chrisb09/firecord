package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class NumericData<T extends Number> extends SmallData<T> {

    NumericData(@NotNull String key, T defaultValue) {
        super(key, defaultValue);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

}
