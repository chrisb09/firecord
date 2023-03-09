package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class LargeData<T> extends SimpleData<T> {

    LargeData(String key, @NotNull T defaultValue, SimpleDataType sdt) {
        super(key, defaultValue, sdt);
    }

    @Override
    int getAggregateTime() {
        // Unload after 60s without use
        return 60000;
    }

    @Override
    int getCacheTime() {
        // Update this key at most 10s after a change somewhere else happened
        return 10000;
    }
    
}
