package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

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
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ListPosition;

@SuppressWarnings("unchecked")
public final class RList<T extends AbstractData<?>> extends CollectionData<T, List<T>> implements List<T> {

    static HashMap<Bytes, RList<AbstractData<?>>> loaded = new HashMap<Bytes, RList<AbstractData<?>>>();

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes addedKey = m.getValue1();
                RList<AbstractData<?>> l = null;
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
                        l.notifyListeners(new ListAddEvent<AbstractData<?>>(sender, l, entry));
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] addedKeys = m.getValue1();
                RList<AbstractData<?>> l = null;
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
                            entries.add(entry);
                        }
                    }
                    l.notifyListeners(new ListAddAllEvent<AbstractData<?>>(sender, l, entries));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_INDEX, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Triplet<Bytes, Integer, Bytes> m = ByteMessage.readIn(message, Bytes.class, Integer.class, Bytes.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes addedKey = m.getValue2();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(addedKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(index, entry);
                        }
                        l.notifyListeners(new ListAddIndexEvent<AbstractData<?>>(sender, l, entry, index));
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_ADD_ALL_INDEX, new MessageReceiver() { // RList_add_all_index

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Triplet<Bytes, Integer, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Integer.class,
                        Bytes[].class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes[] addedKeys = m.getValue2();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    int i = 0;
                    Collection<Object> entries = new ArrayList<>(addedKeys.length);
                    for (Bytes addedKey : addedKeys) {
                        AbstractData<?> entry = AbstractData.create(addedKey);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(index + (i++), entry);
                            }
                            entries.add(entry);
                        }
                    }
                    l.notifyListeners(new ListAddAllIndexEvent<AbstractData<?>>(sender, l, entries, index));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes removedKey = m.getValue1();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> removed = l._removeKey(removedKey);
                    l.notifyListeners(new ListRemoveEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE_INDEX, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Integer> m = ByteMessage.readIn(message, Bytes.class, Integer.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> removed;
                    synchronized (l.data) {
                        removed = l.data.remove(index);
                    }
                    l.notifyListeners(new ListRemoveIndexEvent<AbstractData<?>>(sender, l, removed, index));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_REMOVE_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] removedKeys = m.getValue1();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    List<Object> removed = new ArrayList<>(removedKeys.length);
                    for (Bytes removedKey : removedKeys) {
                        Object r = l._removeKey(removedKey);
                        if (r != null) {
                            removed.add(r);
                        }
                    }
                    l.notifyListeners(new ListRemoveAllEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_RETAIN_ALL, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes[]> m = ByteMessage.readIn(message, Bytes.class, Bytes[].class);
                Bytes key = m.getValue0();
                Bytes[] retainedKeys = m.getValue1();
                RList<AbstractData<?>> l = null;
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
                        if (l.hasListeners()) {
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
                        }
                        l.data.retainAll(toRetain);
                    }
                    l.notifyListeners(new ListRetainAllEvent<AbstractData<?>>(sender, l, removed));
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_SET, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Triplet<Bytes, Integer, Bytes> m = ByteMessage.readIn(message, Bytes.class, Integer.class, Bytes.class);
                Bytes key = m.getValue0();
                int index = m.getValue1();
                Bytes newKey = m.getValue2();
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(newKey);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.set(index, entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.LIST_LOG, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Bytes key = message;
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    System.out.println(key + ":");
                    System.out.println("  cache: " + String.join(",",
                            l.stream().map(bytearray -> bytearray.getKey().toString()).toList()));
                    try (Jedis j = ClassicJedisPool.getJedis()) {
                        System.out.println("  redis: " + String.join(",", j.lrange(key.getData(), 0, -1).stream()
                                .map(bytearray -> new Bytes(bytearray).toString()).toList()));
                    }
                }
            }

        });

    }

    public RList(@NotNull Bytes key) {
        super(key, DataType.LIST);
        synchronized (loaded) {
            loaded.put(key, (RList<AbstractData<?>>) this);
        }
    }

    @Override
    void _load() {
        if (this.data == null) {
            this.data = new ArrayList<T>();
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            List<byte[]> keys = j.lrange(this.key.getData(), 0, -1);
            synchronized (this.data) {
                this.data.clear();
                for (byte[] k : keys) {
                    AbstractData<?> ad = AbstractData.create(new Bytes(k));
                    if (ad != null) {
                        // since load is called AFTER we put this object into the AbstractData loaded
                        // map, cyclical dependencies are handled correctly
                        this.data.add((T) ad);
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
                j.rpush(this.key.getData(),
                        this.data.stream().map(element -> element.getKey().getData()).toArray(byte[][]::new));
            }
        }
    }

    public boolean add(T arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        synchronized (this.data) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.rpush(this.key.getData(), arg0.getKey().getData());
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD,
                    ByteMessage.write(this.key, arg0.getKey()));
            boolean result = this.data.add(arg0);
            this.notifyListeners(new ListAddEvent<AbstractData<?>>(Firecord.getId(),
                    this, arg0));
            return result;
        }
    }

    public void add(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            if (arg0 == this.data.size()) {
                j.rpush(this.key.getData(), arg1.getKey().getData());
            } else {
                j.linsert(key.getData(), ListPosition.BEFORE, this.data.get(arg0).getKey().getData(),
                        arg1.getKey().getData());
            }
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_INDEX,
                ByteMessage.write(this.key, arg0, arg1.getKey()));

        this.notifyListeners(new ListAddIndexEvent<AbstractData<?>>(Firecord.getId(), this, arg1, arg0));
        synchronized (this.data) {
            this.data.add(arg0, arg1);
        }
    }

    public boolean addAll(Collection<? extends T> arg0) {
        if (arg0 == null || arg0.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            byte[][] keys = new byte[arg0.size()][];
            int index = 0;
            for (T entry : arg0) {
                keys[index++] = entry.getKey().getData();
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.rpush(key.getData(), keys);
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL,
                    ByteMessage.write(this.key, arg0.stream().map(entry -> entry.getKey()).toArray(Bytes[]::new)));
            boolean result;
            synchronized (this.data) {
                result = this.data.addAll(arg0);
            }
            this.notifyListeners(new ListAddAllEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
            return result;
        }
    }

    /**
     * This function should always be used with a lock!
     */
    public boolean addAll(int arg0, Collection<? extends T> arg1) {

        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        if (arg1 == null || arg1.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            byte[][] keys = new byte[arg1.size()][];
            int index = 0;
            try (Jedis j = ClassicJedisPool.getJedis()) {
                // get suffix
                List<byte[]> suffix = j.lrange(key.getData(), arg0, -1);
                // remove suffix from list
                if (arg0 == 0) {
                    j.del(key.getData());
                } else {
                    j.ltrim(key.getData(), 0, arg0 - 1);
                }
                for (T entry : arg1) {
                    keys[index++] = entry.getKey().getData();
                }
                // add new entries
                j.rpush(key.getData(), keys);
                // readd suffix
                j.rpush(key.getData(), suffix.toArray(byte[][]::new));
            }
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_ADD_ALL_INDEX, ByteMessage.write(this.key, arg0,
                    arg1.stream().map(entry -> entry.getKey()).toArray(Bytes[]::new)));
            boolean result;
            synchronized (this.data) {
                result = this.data.addAll(arg0, arg1);
            }
            this.notifyListeners(new ListAddAllIndexEvent<AbstractData<?>>(Firecord.getId(), this, arg1, index));
            return result;
        }
    }

    public T get(int arg0) {
        synchronized (this.data) {
            return this.data.get(arg0);
        }
    }

    public int indexOf(Object arg0) {
        synchronized (this.data) {
            return this.data.indexOf(arg0);
        }
    }

    public int lastIndexOf(Object arg0) {
        synchronized (this.data) {
            return this.data.lastIndexOf(arg0);
        }
    }

    public ListIterator<T> listIterator() {
        synchronized (this.data) {
            return this.data.listIterator();
        }
    }

    public ListIterator<T> listIterator(int arg0) {
        synchronized (this.data) {
            return this.data.listIterator(arg0);
        }
    }

    public boolean remove(Object arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        if (!(arg0 instanceof AbstractData)) {
            throw new ClassCastException(
                    arg0.getClass().getName() + " is not an instance of " + AbstractData.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lrem(key.getData(), 1, ((AbstractData<?>) arg0).getKey().getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE,
                ByteMessage.write(this.key, ((T) (arg0)).getKey()));
        boolean result;
        synchronized (this.data) {
            result = this.data.remove(arg0);
        }
        this.notifyListeners(new ListRemoveEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
        return result;
    }

    public List<Integer> indicesOf(T arg0) {
        List<Integer> l = new ArrayList<Integer>();
        synchronized (this.data) {
            for (int i = 0; i < this.data.size(); i++) {
                T element = this.data.get(i);
                if (arg0.equals(element)) {
                    l.add(i);
                }
            }
        }
        return l;
    }

    // Sadly redis has no native function for this. It is paramount to use a lock
    // whenever this function is called.
    public T remove(int arg0) {
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            // get suffix
            List<byte[]> suffix = j.lrange(key.getData(), arg0 + 1, -1);
            // remove suffix from list
            if (arg0 == 0) {
                j.del(key.getData());
            } else {
                j.ltrim(key.getData(), 0, arg0 - 1);
            }
            if (suffix.size() > 0) {
                // readd suffix
                j.rpush(key.getData(), suffix.toArray(byte[][]::new));
            }
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_INDEX,
                ByteMessage.write(this.key, arg0));
        T removed;
        synchronized (this.data) {
            removed = this.data.remove(arg0);
        }
        this.notifyListeners(new ListRemoveIndexEvent<AbstractData<?>>(Firecord.getId(), this, removed, arg0));
        return removed;
    }

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
                    j.lrem(key.getData(), 0, ((T) (element)).getKey().getData());
                }
            }
        }
        boolean result;
        synchronized (arg0) {
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_REMOVE_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
            synchronized (this.data) {
                result = this.data.removeAll(arg0);
            }
        }
        this.notifyListeners(new ListRemoveAllEvent<AbstractData<?>>(Firecord.getId(), this, arg0));
        return result;
    }

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
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_RETAIN_ALL,
                    ByteMessage.write(this.key,
                            arg0.stream().map(entry -> entry instanceof AbstractData ? ((T) (entry)).getKey() : null)
                                    .toArray(Bytes[]::new)));
            try (Jedis j = ClassicJedisPool.getJedis()) {
                for (T element : toRem) {
                    j.lrem(key.getData(), 0, element.getKey().getData());
                }
            }
        }
        boolean result;
        Collection<AbstractData<?>> removed = new ArrayList<>(this.size() - arg0.size());
        synchronized (this.data) {
            result = this.data.retainAll(arg0);
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
        this.notifyListeners(new ListRetainAllEvent<AbstractData<?>>(Firecord.getId(), this, removed));
        return result;
    }

    public T set(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lset(key.getData(), arg0, arg1.getKey().getData());
        }
        JedisCommunication.broadcast(JedisCommunicationChannel.LIST_SET,
                ByteMessage.write(this.key, arg0, arg1.getKey()));
        synchronized (this.data) {
            return this.data.set(arg0, arg1);
        }
    }

    public List<T> subList(int fromIndex, int toIndex) {
        synchronized (this.data) {
            return this.data.subList(fromIndex, toIndex);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry instanceof SimpleInterface) {
                    SimpleInterface<Object> e = (SimpleInterface<Object>) entry;
                    if (e.get().equals(value)) {
                        return true;
                    }
                } else if (entry instanceof CollectionData) {
                    CollectionData<AbstractData<?>, ?> e = (CollectionData<AbstractData<?>, ?>) entry;
                    if (e.data.equals(value)) {
                        return true;
                    }
                } else if (entry instanceof AbstractObject) {
                    return entry.equals(value);
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Bytes key) {
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry.getKey().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<T> getByKey(Bytes key) {
        List<T> result = new ArrayList<>();
        synchronized (this.data) {
            for (T entry : this.data) {
                if (entry.getKey().equals(key)){
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @Override
    public List<T> getByValue(Object value) {
        List<T> result = new ArrayList<>();
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
