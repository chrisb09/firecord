package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class DynamicLargeData<T> extends LargeData<T> {

    /*
     * Replaces the static aggregate and cache timeout values with
     * size dependent ones. Downside is, that this requires some
     * computation, therefore it should only be used on data types
     * that can vary in size from tiny to humongous
     * Because there is a strong correlation between the string size
     * and data size we use it as a proxy for data size
     */

    DynamicLargeData(@NotNull Bytes key, T defaultValue) {
        super(key, defaultValue);
    }

    private int getDataSizeEstimate() {
        if (this.value == null) {
            return 0;
        }
        return this.toBytes().length;
    }

    @Override
    int getAggregateTime() {
        // Update this key at most 5 min after a change somewhere else happened
        return Math.min(5*60*1000, 5000+5*getDataSizeEstimate());
    }

    @Override
    int getCacheTime() {
        // Unload after at most 30 min
        return Math.min(30*60*1000, 60000 + 100 * getDataSizeEstimate());
    }

}
