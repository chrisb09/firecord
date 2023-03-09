package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ListPosition;

@SuppressWarnings("unchecked")
public class RList<T extends AbstractData<?>> extends CompositeData<T, List<T>> implements List<T> {

    static HashMap<String, RList<AbstractData<?>>> loaded = new HashMap<String, RList<AbstractData<?>>>();

    static {

        JedisCommunication.subscribe("RList_add", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                String added_key = new String(Base64.getDecoder().decode(parts[1]));
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(added_key);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe("RList_add_all", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                String[] added_keys = new String[parts.length - 1];
                for (int i = 0; i < added_keys.length; i++) {
                    added_keys[i] = new String(Base64.getDecoder().decode(parts[i + 1]));
                }
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    for (String added_key : added_keys) {
                        AbstractData<?> entry = AbstractData.create(added_key);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(entry);
                            }
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe("RList_add_index", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                int index = Integer.parseInt(parts[1]);
                String added_key = new String(Base64.getDecoder().decode(parts[2]));
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    AbstractData<?> entry = AbstractData.create(added_key);
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(index, entry);
                        }
                    }
                }
            }

        });

        JedisCommunication.subscribe("RList_add_all_index", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                int index = Integer.parseInt(parts[1]);
                String[] added_keys = new String[parts.length - 1];
                for (int i = 0; i < added_keys.length; i++) {
                    added_keys[i] = new String(Base64.getDecoder().decode(parts[i + 2]));
                }
                RList<AbstractData<?>> l = null;
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        l = loaded.get(key);
                    }
                }
                if (l != null) {
                    for (String added_key : added_keys) {
                        AbstractData<?> entry = AbstractData.create(added_key);
                        if (entry != null) {
                            synchronized (l.data) {
                                l.data.add(index, entry);
                            }
                        }
                    }
                }
            }

        });

    }

    private RList(String key, ArrayList<T> data) {
        super(key, data, DataType.LIST);
        synchronized (loaded) {
            loaded.put(key, (RList<AbstractData<?>>) this);
        }
    }

    public RList(String key, int initialCapacity) {
        this(key, new ArrayList<T>(initialCapacity));
    }

    public RList(String key) {
        this(key, new ArrayList<T>());
    }

    @Override
    void _load() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            List<String> keys = j.lrange(this.key, 0, -1);
            System.out.println(this.data);
            synchronized (this.data) {
                this.data.clear();
                for (String k : keys) {
                    AbstractData<?> ad = AbstractData.create(k);
                    if (ad != null) {
                        // since load is called AFTER we put this object into the AbstractData loaded
                        // map cyclical dependencies are handled correctly
                        this.data.add((T) ad);
                    }
                }
            }
        }
    }

    @Override
    void _store() {
        try (AbstractData<T> ad = this.lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.del(this.key);
                j.lpush(this.key, this.data.toArray(new String[0]));
            }
        }
    }

    public boolean add(T arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        synchronized (this.data) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.rpush(this.key, arg0.getKey());
            }
            String p1 = new String(Base64.getEncoder().encode(this.key.getBytes()));
            String p2 = new String(Base64.getEncoder().encode(arg0.getKey().getBytes()));
            JedisCommunication.broadcast("RList_add", p1 + ":" + p2);
            return this.data.add(arg0);
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
                j.rpush(this.key, arg1.getKey());
            } else {
                j.linsert(key, ListPosition.BEFORE, this.data.get(arg0).getKey(), arg1.getKey());
            }
        }
        String p1 = new String(Base64.getEncoder().encode(this.key.getBytes()));
        String p2 = new String(Base64.getEncoder().encode(arg1.getKey().getBytes()));
        JedisCommunication.broadcast("RList_add_index", p1 + ":" + arg0 + ":" + p2);
        synchronized (this.data) {
            this.data.add(arg0, arg1);
        }
    }

    public boolean addAll(Collection<? extends T> arg0) {
        if (arg0 == null || arg0.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            String[] keys = new String[arg0.size()];
            int index = 0;
            for (T entry : arg0) {
                keys[index++] = entry.getKey();
            }
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.lpush(key, keys);
            }

            String p1 = new String(Base64.getEncoder().encode(this.key.getBytes()));
            String[] b64keys = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                b64keys[i] = new String(Base64.getEncoder().encode(keys[i].getBytes()));
            }
            JedisCommunication.broadcast("RList_add_all", p1 + ":" + String.join(":", b64keys));
            synchronized (this.data) {
                return this.data.addAll(arg0);
            }
        }
    }

    @SuppressWarnings("resource")
    public boolean addAll(int arg0, Collection<? extends T> arg1) {

        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        if (arg1 == null || arg1.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            Iterator<? extends T> it = arg1.iterator();
            String[] b64keys = new String[arg1.size()];
            int index = 0;
            try (Jedis j = ClassicJedisPool.getJedis()) {
                T current = it.next();
                b64keys[index++] = new String(Base64.getEncoder().encode(current.getKey().getBytes()));
                if (arg0 == this.data.size()) {
                    j.rpush(this.key, current.getKey());
                } else {
                    j.linsert(key, ListPosition.BEFORE, this.data.get(arg0).getKey(), current.getKey());
                }
                while (it.hasNext()) {
                    T next = it.next();
                    j.linsert(key, ListPosition.AFTER, current.getKey(), next.getKey());
                    b64keys[index++] = new String(Base64.getEncoder().encode(next.getKey().getBytes()));
                    current = next;
                }
            }

            String p1 = new String(Base64.getEncoder().encode(this.key.getBytes()));
            JedisCommunication.broadcast("RList_add_all_index", p1 + ":" + arg0 + ":" + String.join(":", b64keys));
            synchronized (this.data) {
                return this.data.addAll(arg0, arg1);
            }
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
        if (arg0 instanceof AbstractData) {
            throw new ClassCastException(
                    arg0.getClass().getName() + " is not an instance of " + AbstractData.class.getName());
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lrem(key, 1, ((AbstractData<?>) arg0).getKey());
        }
        synchronized (this.data) {
            return this.data.remove(arg0);
        }
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
            T element;
            synchronized (this.data) {
                element = this.data.get(arg0);
            }
            List<Integer> indices = this.indicesOf(element);
            int count = indices.indexOf(arg0);
            // this removes all first count+1 entries
            j.lrem(key, count + 1, key);
            // we now have to readd all first count entries
            for (int i = 0; i < count; i++) {
                int index = indices.get(i);
                String pivot;
                synchronized (this.data) {
                    pivot = this.data.get(index + 1).getKey();
                }
                j.linsert(key, ListPosition.BEFORE, pivot, element.getKey());
            }
        }
        synchronized (this.data) {
            return this.data.remove(arg0);
        }
    }

    public boolean removeAll(Collection<?> arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            synchronized (this.data) {
                for (Object element : arg0) {
                    j.lrem(key, 0, ((T) (element)).getKey());
                }
            }
        }
        synchronized (this.data) {
            return this.data.removeAll(arg0);
        }
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

            try (Jedis j = ClassicJedisPool.getJedis()) {
                for (T element : toRem) {
                    j.lrem(key, 0, element.getKey());
                }
            }
        }
        synchronized (this.data) {
            return this.data.retainAll(arg0);
        }
    }

    public T set(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        if (arg0 < 0 || arg0 > this.data.size()) {
            throw new IndexOutOfBoundsException(arg0);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.lset(key, arg0, arg1.getKey());
        }
        synchronized (this.data) {
            return this.data.set(arg0, arg1);
        }
    }

    public List<T> subList(int fromIndex, int toIndex) {
        synchronized (this.data) {
            return this.data.subList(fromIndex, toIndex);
        }
    }

}
