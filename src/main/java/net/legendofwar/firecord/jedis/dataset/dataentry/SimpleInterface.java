package net.legendofwar.firecord.jedis.dataset.dataentry;

public interface SimpleInterface<T> {

    public T get();

    public boolean set(T value);

    public boolean setIfEmpty(T defaultValue);

}
