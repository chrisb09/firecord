package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.CompositeDataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.SimpleDataType;
import redis.clients.jedis.Jedis;

public abstract class AbstractData<T> implements Closeable {

    private static AbstractData<?> callConstructor(@NotNull String key, @NotNull Class<?> c) {

        try {
            return (AbstractData<?>) c.getDeclaredConstructor(String.class).newInstance(":D");
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
     * @param key
     * @return
     */
    public static AbstractData<?> create(@NotNull String key) {
        
        String type = null;
        try (Jedis j = ClassicJedisPool.getJedis()){
            type = j.type(key);
        }
        if (type != null) {
            for (CompositeDataType cdt : CompositeDataType.values()){
                if (cdt.getRedisName().equals(type)) {
                    return callConstructor(key, cdt.getC());
                }
            }
            if (type.equals("string")){
                String simpleType;
                try (Jedis j = ClassicJedisPool.getJedis()){
                    simpleType = j.get(key+":type");
                }
                if (simpleType != null) {
                    SimpleDataType sdt = SimpleDataType.valueOf(simpleType);
                    return callConstructor(key, sdt.getC());
                }
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

    public String getKey() {
        return key;
    }

    @NotNull
    public abstract Map<String, String> serialize();

}
