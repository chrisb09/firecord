package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListAddAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListAddAllIndexEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListAddEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListAddIndexEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListRemoveAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListRemoveEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListRemoveIndexEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListRetainAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ListSetEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SetAddAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SetAddEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SetRemoveAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SetRemoveEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SetRetainAllEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public final class RSet<T extends AbstractData<?>> extends CollectionData<T, Set<T>> implements Set<T> {


    static HashMap<Bytes, RSet<AbstractData<?>>> loaded = new HashMap<Bytes, RSet<AbstractData<?>>>();

    //TODO: add the code dealing with messages we receive for changes...


    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.SET_ADD, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes addedKey = m.getValue1();
                RSet<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(addedKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(entry);
                        }
                        ArrayList<AbstractData<?>> childOwners = l.owners;
                        synchronized (childOwners) {
                            if (!childOwners.contains(entry)){
                                childOwners.add(entry);
                            }
                        }
                        l.notifyListeners(new SetAddEvent<AbstractData<?>>(sender, l, entry));
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.SET_ADD_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] addedKeys = m.getValue1();
                RSet<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    Collection<Object> entries = new ArrayList<>(addedKeys.length);
                    for (Bytes added_key : addedKeys) {
                        AbstractData<?> entry = AbstractData.create(added_key);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(entry);
                            }
                            ArrayList<AbstractData<?>> childOwners = l.owners;
                            synchronized (childOwners) {   
                                if (!childOwners.contains(entry)){
                                    childOwners.add(entry);
                                }
                            }
                            entries.add(entry);
                        }
                    }
                    l.notifyListeners(new SetAddAllEvent<AbstractData<?>>(sender, l, entries));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.SET_REMOVE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes removedKey = m.getValue1();
                RSet<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> removed = l._removeKey(removedKey);
                    if (removed != null) {
                        ArrayList<AbstractData<?>> childOwners = removed.owners;
                        // the owners/parents of the child
                        synchronized (childOwners) {
                            if (childOwners.contains(l)){
                                childOwners.remove(l);
                                if (childOwners.size()==0){
                                    removed.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                }
                            }
                        }
                    }
                    l.notifyListeners(new SetRemoveEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.SET_REMOVE_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] removedKeys = m.getValue1();
                RSet<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    List<Object> removed = new ArrayList<>(removedKeys.length);
                    for (Bytes removedKey : removedKeys) {
                        AbstractData<?> r = l._removeKey(removedKey);
                        if (r != null) {
                            removed.add(r);
                            ArrayList<AbstractData<?>> childOwners = r.owners;
                            // the owners/parents of the child
                            synchronized (childOwners) {
                                if (childOwners.contains(l)){
                                    childOwners.remove(l);
                                    if (childOwners.size()==0){
                                        r.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                    }
                    l.notifyListeners(new SetRemoveAllEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.SET_RETAIN_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] retainedKeys = m.getValue1();
                RSet<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    List<AbstractData<?>> toRetain = new ArrayList<AbstractData<?>>(retainedKeys.length);
                    for (Bytes retained_key : retainedKeys) {
                        toRetain.add(AbstractData.create(retained_key));
                    }
                    Collection<AbstractData<?>> removed = new ArrayList<>(l.size() - toRetain.size());
                    synchronized (l.data) {
                        if (toRetain.size() > 0) {
                            HashSet<AbstractData<?>> tr = new HashSet<>(toRetain);
                            for (AbstractData<?> ad : l) {
                                if (!tr.contains(ad)) {
                                    removed.add(ad);
                                }
                            }
                        } else {
                            removed.addAll(l.data);
                        }
                        l.data.retainAll(toRetain);
                    }
                    for (AbstractData<?> r : removed){
                        ArrayList<AbstractData<?>> childOwners = r.owners;
                        // the owners/parents of the child
                        synchronized (childOwners) {
                            if (childOwners.contains(l)){
                                childOwners.remove(l);
                                if (childOwners.size()==0){
                                    r.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                                }
                            }
                        }
                    }
                    l.notifyListeners(new SetRetainAllEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

    }


    // cache keys for containsKey ...
    private HashSet<Bytes> dataKeys;

    @SuppressWarnings("unchecked")
    public RSet(@NotNull Bytes key) {
        super(key, DataType.SET);
        synchronized (loaded) {
            loaded.put(key, (RSet<AbstractData<?>>) this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    void _load() {
        if (this.data == null) {
            this.data = new HashSet<T>();
        }
        if (this.dataKeys == null){
            this.dataKeys = new HashSet<>();
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Set<byte[]> keys = j.smembers(this.key.getData());
            synchronized (this.data) {
                this.data.clear();
                this.dataKeys.clear();
                for (byte[] k : keys) {
                    AbstractData<?> ad = AbstractData.create(new Bytes(k));
                    if (ad != null) {
                        // since load is called AFTER we put this object into the AbstractData loaded
                        // map, cyclical dependencies are handled correctly
                        this.data.add((T) ad);
                        this.dataKeys.add(ad.getKey());
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
                j.sadd(this.key.getData(), this.data.stream().map(element -> element.getKey().getData()).toArray(byte[][]::new));
            }
        }
    }

    @Override
    public boolean add(T arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        boolean result;
        synchronized (this.data) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.sadd(this.key.getData(), arg0.getKey().getData());
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.SET_ADD,
                    ByteMessage.write(this.key, arg0.getKey()));
            result = this.data.add(arg0);
            synchronized(this.dataKeys){
                this.dataKeys.add(arg0.getKey());
            }
            ArrayList<AbstractData<?>> childOwners = arg0.owners;
            synchronized (childOwners) {
                if (!childOwners.contains(this)){
                    childOwners.add(this);
                }
            }
            this.notifyListeners(new SetAddEvent<AbstractData<?>>(Firecord.getId(),
                    this, arg0));
            return result;
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        if (arg0 == null || arg0.isEmpty()) { // no change
            return false;
        }
        boolean result;
        synchronized (this.data) {
            byte[][] keys = new byte[arg0.size()][];
            int index = 0;
            for (T entry : arg0) {
                keys[index++] = entry.getKey().getData();
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.sadd(key.getData(), keys);
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.SET_ADD_ALL,
                    ByteMessage.write(this.key, arg0.stream().map(entry -> entry.getKey()).toArray(Bytes[]::new)));
            result = this.data.addAll(arg0);
            synchronized (this.dataKeys) {
                this.dataKeys.addAll(arg0.stream().map(element -> element.getKey()).toList());
            }
            for (T entry : arg0){
                ArrayList<AbstractData<?>> childOwners = entry.owners;
                synchronized (childOwners) {
                    if (!childOwners.contains(this)){
                        childOwners.add(this);
                    }
                }
            }
            this.notifyListeners(new SetAddAllEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (!(arg0 instanceof AbstractData)) {
            throw new ClassCastException(
                    arg0.getClass().getName() + " is not an instance of " + AbstractData.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.srem(key.getData(), ((AbstractData<?>) arg0).getKey().getData());
            
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.SET_REMOVE,
                ByteMessage.write(this.key, ((T) (arg0)).getKey()));
        boolean result;
        synchronized (this.data) {
            result = this.data.remove(arg0);
        }
        synchronized (this.dataKeys){
            if (arg0 instanceof AbstractData ad) {
                this.dataKeys.remove(ad.getKey());
            }
        }
        if (result) {
            ArrayList<AbstractData<?>> childOwners = ((AbstractData<?>) (arg0)).owners;
            // the owners/parents of the child
            synchronized (childOwners) {
                if (childOwners.contains(this)){
                    childOwners.remove(this);
                    if (childOwners.size()==0){
                        ((AbstractData<?>) (arg0)).lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                    }
                }
            }
        }
        this.notifyListeners(new SetRemoveEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeAll(Collection<?> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (arg0.size() == 0) {
            return false;
        }
        byte[][] keys = new byte[arg0.size()][];
        int index = 0;
        synchronized (arg0) {
            for (Object element : arg0) {
                if (element instanceof AbstractData) {
                    @SuppressWarnings("all")
                    AbstractData<?> entry = (AbstractData<?>) element;
                    keys[index++] = entry.getKey().getData();
                }
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.srem(key.getData(), ((T) (element)).getKey().getData());
                }
            }
        }
        boolean result;
        synchronized (arg0) {
            synchronized (this.data) {
            result = this.data.removeAll(arg0);
            }
            synchronized (this.dataKeys){
                this.dataKeys.removeAll(arg0.stream().filter(element -> element instanceof AbstractData).map(element -> ((AbstractData<?>) (element)).getKey()).toList());
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.SET_REMOVE_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
            for (Object obj : arg0){
                if (obj != null && obj instanceof AbstractData) {
                    AbstractData<?> ad = (AbstractData<?>) obj;
                    ArrayList<AbstractData<?>> childOwners = ad.owners;
                    // the owners/parents of the child
                    synchronized (childOwners) {
                        if (childOwners.contains(this)){
                            childOwners.remove(this);
                            if (childOwners.size()==0){
                                ad.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                            }
                        }
                    }
                }
            }
        }
        this.notifyListeners(new SetRemoveAllEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean retainAll(Collection<?> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        List<T> toRem;
        synchronized (this.data) {
            toRem = new ArrayList<>(this.data.size() - arg0.size());
            for (T element : this.data) {
                if (!arg0.contains(element)) {
                    toRem.add(element);
                }
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                for (T element : toRem) {
                    j.srem(key.getData(), element.getKey().getData());
                }
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.SET_RETAIN_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
        }
        boolean result;
        Collection<AbstractData<?>> removed = new ArrayList<>(this.size() - arg0.size());
        synchronized (this.data) {
            result = this.data.retainAll(arg0);
            synchronized(this.dataKeys){
                this.dataKeys.retainAll(arg0.stream().filter(element -> element instanceof AbstractData).map(element -> ((AbstractData<?>) (element)).getKey()).toList());
            }
            if (this.hasListeners()) {
                if (arg0.size() > 0) {
                    HashSet<AbstractData<?>> tr = new HashSet<>(arg0.size());
                    for (AbstractData<?> ad : this.data) {
                        if (!tr.contains(ad)) {
                            removed.add(ad);
                        }
                    }
                } else {
                    removed.addAll(this.data);
                }
            }
        }
        for (AbstractData<?> obj : removed){
            if (obj != null && obj instanceof AbstractData) {
                ArrayList<AbstractData<?>> childOwners = obj.owners;
                // the owners/parents of the child
                synchronized (childOwners) {
                    if (childOwners.contains(this)){
                        childOwners.remove(this);
                        if (childOwners.size()==0){
                            obj.lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
        this.notifyListeners(new SetRetainAllEvent<AbstractData<?>>(Firecord.getId(), this, removed));
        return result;
    }

    @Override
    public boolean containsKey(Bytes key) {
        synchronized (this.dataKeys){
            return this.dataKeys.contains(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (this.data) {
            // shortcut to try to preserve the O(1) lookup, although as this doesnt cover every case it's by definition not O(1)...
            // essentially only works if it is not a simple or collection data type...for now, we could also cover these cases in the future
            //TODO: cache for these cases as well akin to the key-cache
            if (this.data.contains(value)){
                return true;
            }
            for (T entry : this.data) {
                if (entry instanceof SimpleInterface simpleEntry) {
                    if (simpleEntry.get().equals(value)) {
                        return true;
                    }
                } else if (entry instanceof CollectionData collectionEntry) {
                    if (collectionEntry.data.equals(value)) {
                        return true;
                    }
                } else if (entry instanceof AbstractObject) {
                    return entry.equals(value);
                }
            }
        }
        return false;
    }

    //not really that useful in a set
    @Override
    public Set<T> getByKey(Bytes key) {
        Set<T> result = new HashSet<>();
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry.getKey().equals(key)){
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<T> getByValue(Object value) {
        Set<T> result = new HashSet<>();
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry instanceof SimpleInterface) {
                    SimpleInterface<Object> e = (SimpleInterface<Object>) entry;
                    if (e.get().equals(value)) {
                        result.add(entry);
                    }
                } else if (entry instanceof CollectionData) {
                    CollectionData<AbstractData<?>, ?> e = (CollectionData<AbstractData<?>, ?>) entry;
                    if (e.data.equals(value)) {
                        result.add(entry);
                    }
                } else if (entry instanceof AbstractObject) {
                    if (entry.equals(value)){
                        result.add(entry);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean removeByKey(Bytes key) {
        return this.removeAll(getByKey(key));
    }

    @Override
    public boolean removeByValue(Object value) {
        return this.removeAll(getByValue(value));
    }
}
