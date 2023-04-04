package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.UUID;

public final class RUUID extends SmallData<UUID> {

    final static UUID DEFAULT_VALUE = new UUID(0, 0);

    public RUUID(String key) {
        this(key, null);
    }

    public RUUID(String key, UUID defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        this.value = UUID.fromString(value);
    }

    @Override
    public String toString() {
        return "" + this.value;
    }

}
