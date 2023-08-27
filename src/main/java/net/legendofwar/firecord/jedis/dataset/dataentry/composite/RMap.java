package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapClearEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapPutEvent;
import redis.clients.jedis.Jedis;

@SuppressWarnings("unchecked")
public final class RMap<T extends AbstractData<?>> extends CompositeData<T> implements Map<Bytes, T> {

    static HashMap<Bytes, RMap<AbstractData<?>>> loaded = new HashMap<Bytes, RMap<AbstractData<?>>>();

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.MAP_CLEAR, new MessageReceiver() {

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
                    if (l instanceof RMap<?>) {
                        RMap<?> map = ((RMap<?>) (l));
                        Map<Bytes, AbstractData<?>> oldValue;
                        synchronized (map.data) {
                            oldValue = new HashMap<>(map.data);
                            map.data.clear();
                        }
                        synchronized (map.valuesInstance.data) {
                            map.valuesInstance.data.clear();
                        }
                        map.notifyListeners(new MapClearEvent<AbstractData<?>>(sender, l, oldValue));
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.MAP_PUT, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // ByteMessage.write(this.key, arg0.getBytes(), arg1.getKey()));
                Triplet<Bytes, Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes name = m.getValue1();
                Bytes addedKey = m.getValue2();
                RMap<AbstractData<?>> map = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        map = loaded.get(key);
                    }
                }
                if (map != null) {
                    AbstractData<?> entry = AbstractData.create(addedKey);
                    AbstractData<?> removed = null;
                    synchronized (map.data) {
                        if (map.data.containsKey(name)) {
                            removed = map.data.get(name);
                        }
                        map.data.put(name, entry);
                    }
                    synchronized (map.valuesInstance.data) {
                        if (removed != null) {
                            map.valuesInstance.data.remove(removed);
                        }
                        ((ArrayList<AbstractData<?>>) (map.valuesInstance.data)).add(entry);
                    }
                    map.notifyListeners(new MapPutEvent<AbstractData<?>>(sender, map, name, removed, entry));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.MAP_PUT_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // JedisCommunication.broadcast(JedisCommunicationChannel.MAP_PUT_ALL,
                // ByteMessage.write(this.key, mapAsArray));
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] mapAsArray = m.getValue1();
                HashMap<Bytes, Bytes> receivedMap = new HashMap<>(mapAsArray.length / 2);
                for (int i = 0; i < mapAsArray.length; i += 2) {
                    receivedMap.put(mapAsArray[i], mapAsArray[i + 1]);
                }
                RMap<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    for (Map.Entry<Bytes, Bytes> en : receivedMap.entrySet()) {
                        AbstractData<?> entry = AbstractData.create(en.getValue());
                        if (entry != null) {
                            AbstractData<?> removed = null;
                            synchronized (l.data) {
                                if (l.data.containsKey(en.getKey())) {
                                    removed = l.data.get(en.getKey());
                                }
                                l.data.put(en.getKey(), entry);
                            }
                            synchronized (((RMap<?>) (l)).valuesInstance.data) {
                                if (removed != null) {
                                    ((RMap<?>) (l)).valuesInstance.data.remove(removed);
                                }
                                ((ArrayList<AbstractData<?>>) ((RMap<?>) (l)).valuesInstance.data).add(entry);
                            }
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.MAP_REMOVE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes removedName = m.getValue1();
                RMap<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    synchronized (l.data) {
                        AbstractData<?> removed = l.data.remove(removedName);
                        if (removed != null) {
                            synchronized (((RMap<?>) (l)).valuesInstance.data) {
                                ((RMap<?>) (l)).valuesInstance.data.remove(removed);
                            }
                        }
                    }
                }
            }

        });

    }

    HashMap<Bytes, T> data;

    SlaveCollection<T> valuesInstance = new SlaveCollection<T>(this);

    public RMap(@NotNull Bytes key) {
        super(key, DataType.MAP);
        synchronized (loaded) {
            loaded.put(key, (RMap<AbstractData<?>>) this);
        }
    }

    @Override
    public void clear() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(key.getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.MAP_CLEAR, key);
        Map<Bytes, AbstractData<?>> oldValue;
        synchronized (this.data) {
            oldValue = new HashMap<>(this.data);
            this.data.clear();
        }
        synchronized (this.valuesInstance.data) {
            this.valuesInstance.data.clear();
        }
        this.notifyListeners(new MapClearEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
    }

    @Override
    public boolean containsKey(Object arg0) {
        synchronized (this.data) {
            return this.data.containsKey(arg0);
        }
    }

    @Override
    public boolean containsValue(Object arg0) {
        synchronized (this.data) {
            return this.data.containsValue(arg0);
        }
    }

    @Override
    public Set<Entry<Bytes, T>> entrySet() {
        synchronized (this.data) {
            return this.data.entrySet();
        }
    }

    @Override
    public T get(Object arg0) {
        synchronized (this.data) {
            return this.data.get(arg0);
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this.data) {
            return this.data.isEmpty();
        }
    }

    @Override
    public Set<Bytes> keySet() {
        synchronized (this.data) {
            return this.data.keySet();
        }
    }

    @Override
    public T put(Bytes arg0, T arg1) {
        if (arg0 == null || arg1 == null) {
            throw new NullPointerException();
        }
        T replaced = null;
        T result;
        synchronized (this.data) {
            if (this.data.containsKey(arg0)) {
                replaced = this.data.get(arg0);
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.hset(this.key.getData(), arg0.getData(), arg1.getKey().getData());
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.MAP_PUT,
                    ByteMessage.write(this.key, arg0.getBytes(), arg1.getKey()));
            synchronized (this.valuesInstance.data) {
                if (replaced != null) {
                    this.valuesInstance.data.remove(replaced);
                }
                this.valuesInstance.data.add(arg1);
            }
            
            result = this.data.put(arg0, arg1);
        }
        this.notifyListeners(new MapPutEvent<AbstractData<?>>(Firecord.getId(), this, arg0.getBytes(), replaced, arg1));
        return result;
    }

    @Override
    public void putAll(Map<? extends Bytes, ? extends T> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        Map<byte[], byte[]> tempMap = new HashMap<byte[], byte[]>();
        Bytes[] mapAsArray = new Bytes[arg0.size() * 2];
        ArrayList<AbstractData<?>> removed = new ArrayList<AbstractData<?>>();
        List<T> addedValues = new ArrayList<>(arg0.size());
        int index = 0;
        synchronized (this.data) {
            for (Map.Entry<? extends Bytes, ? extends T> en : arg0.entrySet()) {
                mapAsArray[index++] = en.getKey();
                mapAsArray[index++] = en.getValue().getKey();
                tempMap.put(en.getKey().getData(), en.getValue().getKey().getData());
                if (this.data.containsKey(en.getKey())) {
                    removed.add(this.data.get(en.getKey()));
                }
                addedValues.add(en.getValue());
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.hset(this.key.getData(), tempMap);
            }

            JedisCommunication.broadcast(JedisCommunicationChannel.MAP_PUT_ALL,
                    ByteMessage.write(this.key, mapAsArray));
            synchronized (this.valuesInstance.data) {
                for (AbstractData<?> ad : removed) {
                    this.valuesInstance.data.remove(ad);
                }
                this.valuesInstance.data.addAll(addedValues);
            }
            this.data.putAll(arg0);
        }
    }

    @Override
    public T remove(Object arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (!(arg0 instanceof Bytes)) {
            throw new ClassCastException(
                    arg0.getClass().getName() + " is not an instance of " + Bytes.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.hdel(key.getData(), ((Bytes) (arg0)).getData());
            // j.lrem(key.getData(), 1, ((AbstractData<?>) arg0).getKey().getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.MAP_REMOVE,
                ByteMessage.write(this.key, (Bytes) (arg0)));

        synchronized (this.data) {
            if (this.data.containsKey(arg0)) {
                synchronized (this.valuesInstance.data) {
                    this.valuesInstance.data.remove(this.data.get(arg0));
                }
            }
            return this.data.remove(arg0);
        }
    }

    @Override
    public int size() {
        synchronized (this.data) {
            return this.data.size();
        }
    }

    /*
     * Careful:
     * The collection is supposed to be backed by the Map, hence changes to one
     * should be reflected in the other
     * meaning we need to implement a custom collection type to conform with this :/
     * 
     * https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#values--
     */
    @Override
    public Collection<T> values() {
        return this.valuesInstance;
    }

    @Override
    void _load() {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Map<byte[], byte[]> keyMap = j.hgetAll(this.key.getData()); // entry-name, entry-key
            // List<byte[]> keys = j.lrange(this.key.getData(), 0, -1);
            synchronized (this.data) {
                this.data.clear();
                for (byte[] entryName : keyMap.keySet()) {
                    byte[] k = keyMap.get(entryName);
                    AbstractData<?> ad = AbstractData.create(new Bytes(k));
                    if (ad != null) {
                        // since load is called AFTER we put this object into the AbstractData loaded
                        // map, cyclical dependencies are handled correctly
                        this.data.put(new Bytes(entryName), ((T) ad));
                    }
                }
            }
        }
    }

    @Override
    void _store() {
        try (AbstractData<T> ad = this.lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.del(this.key.getData());
                HashMap<byte[], byte[]> byteMap = new HashMap<>(0);
                this.data.forEach((name, obj) -> byteMap.put(name.getData(), obj.getKey().getData()));
                j.hset(this.key.getData(), byteMap);
                // j.rpush(this.key.getData(),
                // this.data.stream().map(element ->
                // element.getKey().getData()).toArray(byte[][]::new));
            }
        }
    }

}
