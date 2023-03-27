package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

public abstract class NumericData<T extends Number> extends SmallData<T> {

    NumericData(String key) {
        super(key);
    }

    public abstract T add(T value);

    public abstract T sub(T value);

    public abstract T mul(T value);

    public abstract T div(T value);

}
