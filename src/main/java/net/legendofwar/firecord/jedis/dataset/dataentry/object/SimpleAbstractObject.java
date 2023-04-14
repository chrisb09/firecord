package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;

public abstract class SimpleAbstractObject<T> extends AbstractObject implements SimpleInterface<T> {

    protected SimpleAbstractObject(@NotNull Bytes key) {
        super(key);
    }

    /**
     * Should return the current value (especially for temp-entries relevant)
     * 
     * @return
     */
    abstract public T getTempValue();

}
