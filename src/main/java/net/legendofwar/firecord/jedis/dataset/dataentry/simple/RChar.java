package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public final class RChar extends SmallData<Character> {

    final static Character DEFAULT_VALUE = ' ';

    public RChar(String key) {
        this(key, null);
    }

    public RChar(String key, Character defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        this.value = value.charAt(0);
    }

    @Override
    public String toString() {
        return "" + this.value;
    }

}
