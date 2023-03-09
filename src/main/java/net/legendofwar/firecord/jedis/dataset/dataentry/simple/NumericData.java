package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class NumericData<T> extends SmallData<T> {

    NumericData(String key, @NotNull T defaultValue, SimpleDataType sdt) {
        super(key, defaultValue, sdt);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

}
