package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public abstract class NumericData<T extends Number> extends SmallData<T> {

    NumericData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

}
