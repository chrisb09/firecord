package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public abstract class CompositeData<T extends AbstractData<?>> extends AbstractData<T> {

    // Map of all (once) loaded Composite entries
    static HashMap<Bytes, CompositeData<?>> loaded = new HashMap<Bytes, CompositeData<?>>();

    CompositeData(@NotNull Bytes key, DataType dt) {
        super(key);
        if (key != null) {
            // make sure the object is NOT a temporary placeholde
            this._load(); // loads data if it exists
            synchronized (loaded) {
                loaded.put(key, this);
            }
            if (this.isEmpty()) {
                this._setType(dt);
            }
        }
    }

    /**
     * Load the data from key
     */
    abstract void _load();

    /**
     * Store the data at key
     */
    abstract void _store();

    abstract public boolean isEmpty();

    abstract public int size();

    abstract public void clear();

}
