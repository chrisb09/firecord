package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public final class RBoolean extends SmallData<Boolean> {

    final static Boolean DEFAULT_VALUE = false;

    public RBoolean(String key) {
        this(key, null);
    }

    public RBoolean(String key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
