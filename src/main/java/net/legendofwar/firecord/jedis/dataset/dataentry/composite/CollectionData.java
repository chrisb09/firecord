package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.CollectionClearEvent;
import redis.clients.jedis.Jedis;

public abstract class CollectionData<T extends AbstractData<?>, E extends Collection<T>> extends CompositeData<T>
        implements Collection<T> {

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.COLLECTION_CLEAR, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Bytes key = message;
                CompositeData<?> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    if (l instanceof CollectionData) {
                        CollectionData<?, ?> cl = ((CollectionData<?, ?>) (l));
                        Collection<?> oldValue;
                        synchronized (cl.data) {
                            oldValue = new ArrayList<>(cl.data);
                            cl.data.clear();
                        }
                        cl.notifyListeners(new CollectionClearEvent<AbstractData<?>>(sender, cl, oldValue));
                    }
                }
            }

        });

    }

    E data;

    CollectionData(@NotNull Bytes key, DataType dt) {
        super(key, dt);
    }

    /**
     * Overwrites the data in this wrapper, example of a "complex" function
     * 
     * @param data
     */
    public void set(E data) {
        try (JedisLock lock = lock()) {
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
    AbstractData<?> _removeKey(Bytes key) {
        synchronized (AbstractData.loaded) {
            if (AbstractData.loaded.containsKey(key)) {
                AbstractData<?> entry = AbstractData.loaded.get(key);
                if (this.data.remove(entry)) {
                    return entry;
                }
            }
        }
        return null;
    }

    @Override
    public void clear() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(key.getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.COLLECTION_CLEAR, key);
        Collection<?> oldValue;
        synchronized (this.data) {
            oldValue = new ArrayList<>(this.data);
            this.data.clear();
        }
        this.notifyListeners(new CollectionClearEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
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

    public abstract E getByKey(Bytes key);

    public abstract E getByValue(Object value);

    /*
     * Removes an Element of this Collection by its key
     * 
     * @param key Bytes instead of RString/String
     * 
     * @return true if element was found and removed
     */
    public abstract boolean removeByKey(Bytes key);

    /*
     * Removes an Element of this Collection by its value
     * 
     * @param value Value of this Collection (String, Byte,...)
     * @return true if element was found and removed
     */
    public abstract boolean removeByValue(Object value);

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
