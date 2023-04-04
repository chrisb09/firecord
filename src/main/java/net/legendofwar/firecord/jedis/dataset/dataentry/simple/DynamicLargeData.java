package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public abstract class DynamicLargeData<T> extends LargeData<T> {

    /*
     * Replaces the satic aggregate and cache timeout values with
     * size dependent ones. Downside is, that this requires some
     * computation, therefore it should only be used on data types
     * that can vary in size from tiny to humongous
     * Because there is a strong correlation between the string size
     * and data size we use it as a proxy for data size
     */

    DynamicLargeData(String key, T defaultValue) {
        super(key, defaultValue);
    }

    private int getDataSizeEstimate() {
        if (this.value == null) {
            return 0;
        }
        return this.toString().length();
    }

    @Override
    int getAggregateTime() {
        // Update this key at most 5 min after a change somewhere else happened
        int tmp = Math.min(5000, getDataSizeEstimate());
        return 10000 + 280000 * tmp / 5000;
    }

    @Override
    int getCacheTime() {
        // Unload after at most 30 min
        return 60000 + 1740000 / (1 + getDataSizeEstimate());
    }

}
