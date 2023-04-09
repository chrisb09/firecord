package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import redis.clients.jedis.Jedis;

public abstract class AbstractData<T> implements Closeable {

    static {

        Firecord.subscribe("del_key", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                AbstractData<?> ad = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)){
                        ad = loaded.get(message);
                    }
                }
                DataGenerator.delete(ad, false);
            }
            
        });

    }

    public static AbstractData<?> callConstructor(@NotNull byte[] key, @NotNull Class<?> c) {

        try {
            return (AbstractData<?>) c.getDeclaredConstructor(byte[].class).newInstance(key);
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

    public static AbstractData<?> callConstructor(@NotNull byte[] key, @NotNull Class<?> c, Object defaultValue) {

        try {
            return (AbstractData<?>) c.getDeclaredConstructor(byte[].class, defaultValue.getClass()).newInstance(key, defaultValue);
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
    public static AbstractData<?> create(@NotNull byte[] key) {

        // since reflection is used the @NotNull annotation does not guarantee null safety
        if (key == null){
            return null;
        }

        // we don't need to recreate
        synchronized (loaded) {
            if (loaded.containsKey(key)) {
                return loaded.get(key);
            }
        }
        String type = null;
        try (Jedis j = ClassicJedisPool.getJedis()) {
            byte[] t = j.get(KeyGenerator.join(key, DataKeySuffix.TYPE.getData()));
            if (t != null){
                type = new String(t);
            }
        }
        if (type != null) {
            DataType dt = null;
            try {
                dt = DataType.valueOf(type);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (dt != null) {
                if (dt != DataType.OBJECT) {
                    if (dt.canBeLoaded()) {
                        return callConstructor(key, dt.getC());
                    } else {
                        return new Invalid(key);
                    }
                } else {
                    String className = null;
                    byte[] cN;
                    try (Jedis j = ClassicJedisPool.getJedis()) {
                        cN = j.get(KeyGenerator.join(key, DataKeySuffix.CLASS.getData()));
                    }
                    if (cN != null) {
                        className = new String(cN);
                    }
                    Class<?> c = null;
                    if (className != null) {
                        try {
                            c = Class.forName(className);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    if (c != null && AbstractObject.class.isAssignableFrom(c)
                            && !Modifier.isAbstract(c.getModifiers())) {
                        return callConstructor(key, c);
                    }
                }
            }
        }
        return null;

    }

    protected final byte[] key;
    protected final JedisLock lock;

    public static HashMap<byte[], AbstractData<?>> loaded = new HashMap<byte[], AbstractData<?>>();

    protected AbstractData(@NotNull byte[] key) {
        this.key = key;
        if (key != null) {
            // make sure the object is NOT a temporary placeholde
            this.lock = new JedisLock(KeyGenerator.getLockKey(key));
            synchronized (loaded) {
                loaded.put(key, this);
            }
        } else {
            lock = null;
        }
    }

    public AbstractData<T> lock() {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).lock();
        }
        this.lock.lock();
        return this;
    }

    @Override
    public void close() {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            AbstractObject.replaceTemp(this).close();
            return;
        }
        this.lock.unlock();
    }

    protected void _setType(Enum<?> dt) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(KeyGenerator.join(key,DataKeySuffix.TYPE.getData()), dt.toString().getBytes());
        }
    }

    public final byte[] getKey() {
        return key;
    }

}
