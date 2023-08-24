package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import redis.clients.jedis.Jedis;

public abstract class AbstractData<T> implements Closeable {

    static {

        Firecord.subscribe(JedisCommunicationChannel.DEL_KEY, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                AbstractData<?> ad = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)) {
                        ad = loaded.get(message);
                    }
                }
                DataGenerator.delete(ad, false);
            }

        });

    }

    public static AbstractData<?> callConstructor(@NotNull Bytes key, @NotNull Class<?> c) {

        try {
            Constructor<?> constr = c.getDeclaredConstructor(Bytes.class);
            constr.setAccessible(true);
            return (AbstractData<?>) constr.newInstance(key);
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

    public static AbstractData<?> callConstructor(@NotNull Bytes key, @NotNull Class<?> c, Object defaultValue, Class<?> defaultValueClass) {

        try {
            Constructor<?> constr = c.getDeclaredConstructor(Bytes.class, defaultValueClass);
            constr.setAccessible(true);
            return (AbstractData<?>) constr.newInstance(key, defaultValue);
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
    public static AbstractData<?> create(@NotNull Bytes key) {

        // since reflection is used the @NotNull annotation does not guarantee null
        // safety
        if (key == null) {
            return null;
        }

        // for null references
        if (key.length == 0){
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
            byte[] t = j.get(ByteFunctions.join(key, DataKeySuffix.TYPE));
            if (t != null) {
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
                        cN = j.get(ByteFunctions.join(key, DataKeySuffix.CLASS));
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

    protected final Bytes key;
    protected final JedisLock lock;

    public static HashMap<Bytes, AbstractData<?>> loaded = new HashMap<Bytes, AbstractData<?>>();

    protected AbstractData(@NotNull Bytes key) {
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

    public JedisLock getJedisLock(){
        return this.lock;
    }

    public boolean tryLockMultiple(JedisLock... partners) {
        return this.lock.tryLockMultiple(partners);
    }

    public void unlockMultiple(JedisLock... partners) {
        this.lock.unlockMultiple(partners);
    }

    @SuppressWarnings("unchecked")
    public <E extends AbstractData<T>> E lock(E object) {
        return (E) this.lock();
    }

    public AbstractData<T> lock() {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        this.lock.lock();
        return this;
    }

    @Override
    public void close() {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        this.lock.unlock();
    }

    protected void _setType(Enum<?> dt) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(ByteFunctions.join(key, DataKeySuffix.TYPE), dt.toString().getBytes());
        }
    }

    public final Bytes getKey() {
        return key;
    }

    protected void printTempErrorMsg(){
        System.out.println("Temporary Entries (key=null) cannot use their set, lock or get (basically any) functions, they only serve to initialize fields. Temporary entries are intended for initializing fields of objects inheriting from AbstractObject. They are automatically replaced with a fitting entry when using 'field = tempEntry' for a field of an AbstractObject. The key will also be null if this object has been overwritten by a new entry generated by a tempEntry.");
    }

}
