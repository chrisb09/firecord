package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public class RString extends SmallData<String> {

    final static String DEFAULT_VALUE = "";

    public RString(String key) {
        super(key);
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
