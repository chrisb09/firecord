package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import redis.clients.jedis.Jedis;

public abstract class AbstractData<T> implements Closeable {

    private static AbstractData<?> callConstructor(@NotNull String key, @NotNull Class<?> c) {

        try {
            return (AbstractData<?>) c.getDeclaredConstructor(String.class).newInstance(key);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates the corresponding data structure for a key - if it exists that is.
     * If we already have loaded the corresponding datastructure return that
     * instead.
     * If you need a second instance for the same object for whatever terrible
     * reason do call the constructor.
     * 
     * @param key
     * @return
     */
    public static AbstractData<?> create(@NotNull String key) {
        // we don't need to recreate
        synchronized (loaded) {
            if (loaded.containsKey(key)) {
                return loaded.get(key);
            }
        }
        String type = null;
        try (Jedis j = ClassicJedisPool.getJedis()) {
            type = j.get(key + ":type");
        }
        if (type != null) {
            DataType dt = null;
            try {
                dt = DataType.valueOf(type);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (dt != null) {
                return callConstructor(key, dt.getC());
            }
        }
        return null;

    }

    protected final String key;
    protected final JedisLock lock;

    public static HashMap<String, AbstractData<?>> loaded = new HashMap<String, AbstractData<?>>();

    protected AbstractData(String key) {
        this.key = key;
        this.lock = new JedisLock(key + ":lock");
        synchronized (loaded) {
            loaded.put(key, this);
        }
    }

    public AbstractData<T> lock() {
        this.lock.lock();
        return this;
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    protected void _setType(String key, Enum<?> dt) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(key + ":type", dt.toString());
        }
    }

    public final String getKey() {
        return key;
    }

}
