package net.legendofwar.firecord.jedis.dataset.dataentry.type;

import org.jetbrains.annotations.NotNull;

public abstract class SmallData<T> extends SimpleData<T> {

    SmallData(String key, @NotNull T defaultValue, long cache_time, long aggregate_time) {
        super(key, defaultValue, cache_time, aggregate_time);
    }

    SmallData(String key, @NotNull T defaultValue, boolean smallEntry) {
        super(key, defaultValue, smallEntry);
    }

    SmallData(String key, @NotNull T defaultValue) {
        super(key, defaultValue, true);
    }

}
