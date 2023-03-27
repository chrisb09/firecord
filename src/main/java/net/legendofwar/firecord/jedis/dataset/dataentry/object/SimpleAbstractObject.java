package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;

public abstract class SimpleAbstractObject<T> extends AbstractObject implements SimpleInterface<T> {

    protected SimpleAbstractObject(String key) {
        super(key);
    }
    
}
