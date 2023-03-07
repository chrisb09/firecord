package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

public abstract class LargeData<T> extends SimpleData<T> {

    LargeData(String key, @NotNull T defaultValue) {
        // Unload after 30s
        // Update this key at most 10s after a change somewhere else happened
        super(key, defaultValue, 30l * 1000l * 1000l * 1000l, 10l * 1000l * 1000l * 1000l);
    }
    
}
