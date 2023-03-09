package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public abstract class SmallData<T> extends SimpleData<T> {

    SmallData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
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
