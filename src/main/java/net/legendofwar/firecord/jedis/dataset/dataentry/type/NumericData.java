package net.legendofwar.firecord.jedis.dataset.dataentry.type;

import org.jetbrains.annotations.NotNull;

public abstract class NumericData<T> extends SmallData<T> {

    NumericData(String key, @NotNull T defaultValue, long cache_time, long aggregate_time) {
        super(key, defaultValue, aggregate_time, aggregate_time);
    }

    NumericData(String key, @NotNull T defaultValue, boolean smallEntry) {
        this(key, defaultValue, smallEntry ? 0 : 30 * 1000 * 1000 * 3000, smallEntry ? 0 : 10 * 1000 * 1000 * 3000);
    }

    NumericData(String key, @NotNull T defaultValue) {
        this(key, defaultValue, true);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

}
