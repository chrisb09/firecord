package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public abstract class CompositeData<T extends AbstractData<?>> extends AbstractData<T> implements Collection<T> {

    // Map of all (once) loaded Composite entries
    static HashMap<String, CompositeData<?>> loaded = new HashMap<String, CompositeData<?>>();

    final Collection<T> data;

    CompositeData(String key, Collection<T> data) {
        super(key);
        this.data = data;
        synchronized (loaded) {
            loaded.put(key, this);
        }
    }

    @Override
    public void clear() {
        try (AbstractData<T> ad = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.del(key);
            }
            JedisCommunication.broadcast("RCollection_del", key);
            synchronized (this.data) {
                this.data.clear();
            }
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
    public <E> E[] toArray(E[] arg0) {
        synchronized (this.data) {
            return this.data.toArray(arg0);
        }
    }

}
