package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;

public abstract class CompositeData<T extends AbstractData<?>, E extends Collection<T>> extends AbstractData<T>
        implements Collection<T> {

    // Map of all (once) loaded Composite entries
    static HashMap<String, CompositeData<?, ?>> loaded = new HashMap<String, CompositeData<?, ?>>();

    static {

        JedisCommunication.subscribe("RCollection_del", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String key = new String(Base64.getDecoder().decode(message));
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

    CompositeData(String key, E data, DataType dt) {
        super(key);
        this.data = data; // should be empty at this point
        this._load(); // loads data if it exists
        synchronized (loaded) {
            loaded.put(key, this);
        }
        if (this.isEmpty()) {
            this._setType(key, dt);
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

    @Override
    public void clear() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(key);
        }
        JedisCommunication.broadcast("RCollection_del", new String(Base64.getEncoder().encode(key.getBytes())));
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
