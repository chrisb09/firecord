package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class SmallData<T> extends SimpleData<T> {

    SmallData(String key, @NotNull T defaultValue, SimpleDataType sdt) {
        super(key, defaultValue, sdt);
    }

    @Override
    int getAggregateTime() {
        return 0;
    }

    @Override
    int getCacheTime() {
        return 0;
    }

}
