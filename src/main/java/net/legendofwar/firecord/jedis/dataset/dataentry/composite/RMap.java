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
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapClearEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapPutAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapPutEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.MapRemoveEvent;
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
                            for (Map.Entry<Bytes, ?> en : map.data.entrySet()){
                                if (en.getValue() != null && en.getValue() instanceof AbstractData<?>){
                                    ArrayList<AbstractData<?>> childOwners = ((AbstractData<?>) en.getValue()).owners;
                                    // the owners/parents of the child
                                    synchronized (childOwners) {
                                        if (childOwners.contains(map)){
                                            childOwners.remove(map);
                                        }
                                        ((AbstractData<?>) en.getValue()).lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                    }
                                }
                            }
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
                    if (entry == null) {
                        System.out.println("Map put receive message got key "+addedKey.toString()+" (redis:"+addedKey.toRedisString()+") which doesnt seem to have to have data in the db.");
                    }
                    AbstractData<?> removed = null;
                    synchronized (map.data) {
                        if (map.data.containsKey(name)) {
                            removed = map.data.get(name);
                        }
                        map.data.put(name, entry);
                        synchronized (entry.owners) {
                            entry.owners.add(map);
                        }
                    }
                    synchronized (map.valuesInstance.data) {
                        if (removed != null) {
                            int previousSize;
                            synchronized (removed.owners) {
                                previousSize = removed.owners.size();
                                removed.owners.remove(map);
                            }
                            if (previousSize>0 && removed.owners.size()==0){
                                removed.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                            }
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
                RMap<AbstractData<?>> map = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        map = loaded.get(key);
                    }
                }
                if (map != null) {
                    Map<Bytes, AbstractData<?>> entriesRemoved = new HashMap<>();
                    Map<Bytes, AbstractData<?>> entriesReceived = new HashMap<>();
                    for (Map.Entry<Bytes, Bytes> en : receivedMap.entrySet()) {
                        AbstractData<?> entry = AbstractData.create(en.getValue());
                        AbstractData<?> removed = null;
                        synchronized (map.data) {
                            if (map.data.containsKey(en.getKey())) {
                                removed = map.data.get(en.getKey());
                                entriesRemoved.put(en.getKey(), removed);
                            }
                            map.data.put(en.getKey(), entry);
                            entriesReceived.put(en.getKey(), entry);
                            synchronized (entry.owners) {
                                entry.owners.add(map);
                            }
                        }
                        synchronized (map.valuesInstance.data) {
                            if (removed != null) {
                                int previousSize;
                                synchronized (removed.owners) {
                                    previousSize = removed.owners.size();
                                    removed.owners.remove(map);
                                }
                                if (previousSize>0 && removed.owners.size()==0){
                                    removed.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                }
                                map.valuesInstance.data.remove(removed);
                            }
                            ((ArrayList<AbstractData<?>>) ((RMap<?>) (map)).valuesInstance.data).add(entry);
                        }
                    }
                    map.notifyListeners(new MapPutAllEvent<AbstractData<?>>(Firecord.getId(), map, entriesRemoved, entriesReceived));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.MAP_REMOVE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes removedName = m.getValue1();
                RMap<AbstractData<?>> map = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        map = loaded.get(key);
                    }
                }
                if (map != null) {
                    AbstractData<?> removed;
                    synchronized (map.data) {
                        removed = map.data.remove(removedName);
                        if (removed != null) {
                            synchronized (removed.owners) {
                                int previousSize = removed.owners.size();
                                removed.owners.remove(map);
                                if (previousSize>0 && removed.owners.size()==0){
                                    removed.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                }
                            }
                            synchronized (map.valuesInstance.data) {
                                map.valuesInstance.data.remove(removed);
                            }
                        }
                    }
                    map.notifyListeners(new MapRemoveEvent<AbstractData<?>>(sender, map, removedName, removed));
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
            for (Map.Entry<Bytes, ?> en : data.entrySet()){
                if (en.getValue() != null && en.getValue() instanceof AbstractData<?>){
                    ArrayList<AbstractData<?>> childOwners = ((AbstractData<?>) en.getValue()).owners;
                    // the owners/parents of the child
                    synchronized (childOwners) {
                        if (childOwners.contains(this)){
                            childOwners.remove(this);
                        }
                        ((AbstractData<?>)(en.getValue())).lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                    }
                }
            }
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
                int previousSize;
                synchronized (replaced.owners){
                    previousSize = replaced.owners.size();
                    replaced.owners.remove(this);
                }
                if (previousSize>0 && replaced.owners.size()==0){
                    replaced.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                }
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.hset(this.key.getData(), arg0.getData(), arg1.getKey().getData());
            }
            synchronized (owners) {
                arg1.owners.add(this);
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
        Map<Bytes, AbstractData<?>> removed = new HashMap<Bytes, AbstractData<?>>();
        List<T> addedValues = new ArrayList<>(arg0.size());
        int index = 0;
        synchronized (this.data) {
            for (Map.Entry<? extends Bytes, ? extends T> en : arg0.entrySet()) {
                mapAsArray[index++] = en.getKey();
                mapAsArray[index++] = en.getValue().getKey();
                tempMap.put(en.getKey().getData(), en.getValue().getKey().getData());
                if (this.data.containsKey(en.getKey())) {
                    removed.put(en.getKey(), this.data.get(en.getKey()));
                    if (this.data.size() == 1) {
                        this.data.get(en.getKey()).lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                    }
                    synchronized (this.data.get(en.getKey()).owners) {
                        this.data.get(en.getKey()).owners.remove(this);
                    }
                }
                addedValues.add(en.getValue());
                synchronized (en.getValue().owners) {
                    en.getValue().owners.add(this);
                }
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.hset(this.key.getData(), tempMap);
            }

            JedisCommunication.broadcast(JedisCommunicationChannel.MAP_PUT_ALL,
                    ByteMessage.write(this.key, mapAsArray));
            synchronized (this.valuesInstance.data) {
                for (AbstractData<?> ad : removed.values()) {
                    this.valuesInstance.data.remove(ad);
                }
                this.valuesInstance.data.addAll(addedValues);
            }
            this.data.putAll(arg0);
            this.notifyListeners(new MapPutAllEvent<AbstractData<?>>(Firecord.getId(), this, removed, (Map<Bytes, ?>) arg0));
        }
    }

    @Override
    public T remove(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (!(key instanceof Bytes)) {
            throw new ClassCastException(
                    key.getClass().getName() + " is not an instance of " + Bytes.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.hdel(this.key.getData(), ((Bytes) (key)).getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.MAP_REMOVE,
                ByteMessage.write(this.key, (Bytes) (key)));

        T result;
        Bytes removedName = (Bytes) key;
        synchronized (this.data) {
            if (this.data.containsKey(key)) {
                synchronized (this.valuesInstance.data) {
                    this.valuesInstance.data.remove(this.data.get(key));
                }
            }
            result = this.data.remove(key);
        }
        synchronized (result.owners) {
            result.owners.remove(this);
        }
        this.notifyListeners(new MapRemoveEvent<AbstractData<?>>(Firecord.getId(), this, removedName, result));
        return result;
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
        try (JedisLock lock = this.lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.del(this.key.getData());
                HashMap<byte[], byte[]> byteMap = new HashMap<>(0);
                this.data.forEach((name, obj) -> byteMap.put(name.getData(), obj.getKey().getData()));
                j.hset(this.key.getData(), byteMap);
            }
        }
    }

    @Override
    public void deleteChild(AbstractData<?> ad) {
        try (JedisLock lock = this.lock()){
            for (Map.Entry<Bytes, T> en : this.data.entrySet()){
                if (en.getValue().equals(ad)){
                    this.remove(en.getKey());
                }
            }
        }
    }

}
