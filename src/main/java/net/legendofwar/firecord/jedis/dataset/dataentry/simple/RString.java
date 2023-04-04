package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public class RString extends SmallData<String> {

    final static String DEFAULT_VALUE = "";

    public RString(String key) {
        this(key, null);
    }

    public RString(String key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
