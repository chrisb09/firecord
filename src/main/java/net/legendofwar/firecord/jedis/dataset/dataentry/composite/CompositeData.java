package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;

public abstract class CompositeData<T extends AbstractData<?>, E extends Collection<T>> extends AbstractData<T>
        implements Collection<T> {

    // Map of all (once) loaded Composite entries
    static HashMap<Bytes, CompositeData<?, ?>> loaded = new HashMap<Bytes, CompositeData<?, ?>>();

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.DEL_COLLECTION, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Bytes key = message;
                CompositeData<?, ?> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    synchronized (l.data) {
                        l.data.clear();
                    }
                }
            }

        });

    }

    final E data;

    CompositeData(@NotNull Bytes key, E data, DataType dt) {
        super(key);
        this.data = data; // should be empty at this point
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

    /**
     * Overwrites the data in this wrapper, example of a "complex" function
     * 
     * @param data
     */
    public void set(E data) {
        try (AbstractData<?> ad = lock()) {
            this.clear();
            this.addAll(data);
        }
    }

    /**
     * Removes the first matching entry from this composite type identfied by the
     * key
     * 
     * @param key
     * @return true if an element was removed
     */
    public boolean removeKey(Bytes key) {
        synchronized (AbstractData.loaded) {
            if (AbstractData.loaded.containsKey(key)) {
                AbstractData<?> entry = AbstractData.loaded.get(key);
                return this.remove(entry);
            }
        }
        return false;
    }

    /**
     * Edit data directly, does not send notifications to other nodes
     * 
     * @param key
     * @return
     */
    boolean _removeKey(Bytes key) {
        synchronized (AbstractData.loaded) {
            if (AbstractData.loaded.containsKey(key)) {
                AbstractData<?> entry = AbstractData.loaded.get(key);
                return this.data.remove(entry);
            }
        }
        return false;
    }

    @Override
    public void clear() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(key.getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.DEL_COLLECTION, key);
        synchronized (this.data) {
            this.data.clear();
        }
    }

    @Override
    public boolean contains(Object arg0) {
        synchronized (this.data) {
            return this.data.contains(arg0);
        }
    }

    @Override
    public int hashCode() {
        synchronized (this.data) {
            return this.data.hashCode();
        }
    }

    public abstract boolean containsKey(Bytes key);

    public abstract boolean containsValue(Object value);

    @Override
    public boolean containsAll(Collection<?> arg0) {
        synchronized (this.data) {
            return this.data.containsAll(arg0);
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this.data) {
            return this.data.isEmpty();
        }
    }

    @Override
    public Iterator<T> iterator() {
        synchronized (this.data) {
            return this.data.iterator();
        }
    }

    @Override
    public int size() {
        synchronized (this.data) {
            return this.data.size();
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (this.data) {
            return this.data.toArray();
        }
    }

    @Override
    public <F> F[] toArray(F[] arg0) {
        synchronized (this.data) {
            return this.data.toArray(arg0);
        }
    }

}
