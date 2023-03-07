package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class SmallData<T> extends SimpleData<T> {

    SmallData(String key, @NotNull T defaultValue) {
        super(key, defaultValue, 0l, 0l);
    }

}
