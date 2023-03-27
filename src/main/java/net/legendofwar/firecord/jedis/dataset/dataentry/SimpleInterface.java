package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.util.function.Consumer;

public interface SimpleInterface<T> {

    public T get();

    public boolean set(T value);

    public boolean setIfEmpty(T defaultValue);

    public void listen(Consumer<SimpleInterface<T>> listener);

}
