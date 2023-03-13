package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
public abstract class IntegerData<T extends Number> extends NumericData<T> {

    IntegerData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
    }

    public abstract T incr(T value);

}
