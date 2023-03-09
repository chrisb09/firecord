package net.legendofwar.firecord.jedis.dataset.dataentry.composite;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ListPosition;

@SuppressWarnings("unchecked")
public class RList<T extends AbstractData<?>> extends CompositeData<T> implements List<T>{

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
                    AbstractData<?> entry = null;
                    synchronized (AbstractData.loaded) {
                        if (AbstractData.loaded.containsKey(added_key)) {
                            entry = AbstractData.loaded.get(added_key);
                        }
                    }
                    if (entry == null) {
                        entry = AbstractData.create(added_key);
                    }
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
                        AbstractData<?> entry = null;
                        synchronized (AbstractData.loaded) {
                            if (AbstractData.loaded.containsKey(added_key)) {
                                entry = AbstractData.loaded.get(added_key);
                            }
                        }
                        if (entry == null) {
                            entry = AbstractData.create(added_key);
                        }
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
                    AbstractData<?> entry = null;
                    synchronized (AbstractData.loaded) {
                        if (AbstractData.loaded.containsKey(added_key)) {
                            entry = AbstractData.loaded.get(added_key);
                        }
                    }
                    if (entry == null) {
                        entry = AbstractData.create(added_key);
                    }
                    if (entry != null) {
                        synchronized (l.data) {
                            l.data.add(index, entry);
                        }
                    }
                }
            }

        });

    }

    private RList(String key, ArrayList<T> data) {
        super(key, data);
        synchronized (loaded) {
            loaded.put(key, (RList<AbstractData<?>>) this);
        }
    }

    public RList(String key, int initialCapacity) {
        this(key, new ArrayList<T>(initialCapacity));
        _load();
    }

    public RList(String key) {
        this(key, new ArrayList<T>());
        _load();
    }

    private void _load() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            if (j.type(this.key).equals(CompositeDataType.LIST.redisName)) {
                List<String> keys = j.lrange(this.key, 0, -1);
                synchronized (data) {
                    this.data.clear();
                    for (String k : keys) {
                        AbstractData<?> ad = AbstractData.create(k);
                        if (ad != null) {
                            this.data.add((T) ad);
                        }
                    }
                }
            } else {
                // no entry ...
            }
        }
    }

    public ArrayList<T> data;

    public boolean add(T arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
        synchronized (this.data) {
            try (AbstractData<T> ad = lock()) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.rpush(this.key, arg0.getKey());
                }
                String p1 = new String(Base64.getEncoder().encode(this.key.getBytes()));
                String p2 = new String(Base64.getEncoder().encode(arg0.getKey().getBytes()));
                JedisCommunication.broadcast("RList_add", p1 + ":" + p2);
                return this.data.add(arg0);
            }
        }
    }

    public void add(int arg0, T arg1) {
        if (arg1 == null) {
            throw new NullPointerException();
        }
        synchronized (this.data) {
            try (AbstractData<T> ad = lock()) {
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
                this.data.add(arg0, arg1);
            }
        }
    }

    public boolean addAll(Collection<? extends T> arg0) {
        if (arg0 == null || arg0.isEmpty()) { // no change
            return false;
        }
        synchronized (this.data) {
            try (AbstractData<T> ad = lock()) {
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
            }
            return this.data.addAll(arg0);
        }
    }

    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addAll'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    public T remove(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    public boolean removeAll(Collection<?> arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAll'");
    }

    public boolean retainAll(Collection<?> arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retainAll'");
    }

    public T set(int arg0, T arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'set'");
    }

    public List<T> subList(int fromIndex, int toIndex) {
        synchronized (this.data) {
            return this.data.subList(fromIndex, toIndex);
        }
    }


    @Override
    public @NotNull Map<String, String> serialize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'serialize'");
    }

}
