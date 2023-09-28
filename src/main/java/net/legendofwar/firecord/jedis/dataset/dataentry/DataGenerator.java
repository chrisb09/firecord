package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.CollectionData;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.CompositeData;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RMap;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import redis.clients.jedis.Jedis;

public class DataGenerator<T extends AbstractData<?>> {

    static List<DataGenerator<?>> dataPools = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static void delete(AbstractData<?> ad, boolean deleteInDB) {

        if (ad == null){
            return;
        }

        // only allow deletion for generated values (1)
        // don't delete already deleted (2)
        if ((ad.modifier & 1) == 0 || (ad.modifier & 2) == 1){
            return;
        }

        // marks as already deleted
        ad.modifier = ad.modifier | 2;
        
        // remove from pools curated list
        synchronized (dataPools) {
            for (DataGenerator<?> dp : dataPools) {
                synchronized (dp.curated) {
                    dp.curated.remove(ad.getKey());
                    try (Jedis j = Firecord.getJedis()) {
                        j.srem(dp.key.getData(), ad.getKey().getData());
                    }
                }

            }
        }
        // actually delete entries in DB
        if (deleteInDB) {
            del(ad.getKey());
            // send firecord message to other nodes
            Firecord.broadcast(JedisCommunicationChannel.DEL_KEY, ad.getKey());
        }

        // Delete
        Class<?> c = ad.getClass();
        while (c != AbstractData.class) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (field.getName().equals("loaded")) {
                        field.setAccessible(true);
                        try {
                            HashMap<Bytes, ?> loaded = (HashMap<Bytes, ?>) (field.get(null));
                            synchronized (loaded) {
                                loaded.remove(ad.getKey());
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (AbstractObject.class.isAssignableFrom(c)) {
                    if (SimpleInterface.class.isAssignableFrom(field.getType())) {
                        // RWrapper special case i guess
                        AbstractData<?> si = null;
                        try {
                            si = (AbstractData<?>) field.get(ad);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if (si != null) {
                            delete(si, deleteInDB);
                        }
                    } else if (!c.equals(AbstractData.class)){
                        field.setAccessible(true);
                        try {
                            AbstractObject object = (AbstractObject) field.get(ad);
                            if (object != null){
                                delete(object, deleteInDB);
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } 
            }
            c = c.getSuperclass();
        }

        if (ad instanceof CompositeData){
            if (ad instanceof CollectionData){
                try (JedisLock lock = ad.lock()){
                    for (AbstractData<?> entry : ((CollectionData<?,?>) (ad))){
                        delete(entry, deleteInDB);
                    }
                }
            } else if (ad instanceof RMap){
                for (Map.Entry<Bytes,AbstractData<?>> entry : ((RMap<AbstractData<?>>) (ad)).entrySet()){
                    if (entry.getValue() != null){
                        delete(entry.getValue(), deleteInDB);
                    }
                }
            }
        }

    }

    public static void delete(AbstractData<?> ad) {
        delete(ad, true);
    }

    private static void del(Bytes key) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            for (DataKeySuffix dks : DataKeySuffix.values()) {
                j.del(ByteFunctions.join(key, dks));
            }
            j.del(key.getData());
        }
    }

    final Bytes key;
    final Thread thread;
    final DataType type;
    final Class<T> c;

    Map<Bytes, T> curated = new HashMap<Bytes, T>();

    public DataGenerator(Bytes key, Class<T> c) {
        this(key, c, true);
    }

    public DataGenerator(String name, Class<T> c){
        this(KeyGenerator.getDataGeneratorKey(name), c);
    }
    
    /**
     * 
     * @param key
     * @param manualMemoryManagement true means there will be no automatic deletion
     */

    public DataGenerator(Bytes key, Class<T> c, boolean manualMemoryManagement) {
        synchronized (dataPools) {
            dataPools.add(this);
        }
        this.key = key;
        DataType t = DataType.getByC(c);
        if (t == null) {
            if (AbstractObject.class.isAssignableFrom(c)){
                t = DataType.OBJECT;
            } else {
                throw new InvalidParameterException("Class " + c + " is not registered in DataType.");
            }
        }
        this.type = t;
        if (Modifier.isAbstract(c.getModifiers())) {
            throw new InvalidParameterException(
                    "The class " + c + " is abstract. We cannot create objects of an abstract class");
        }
        this.c = c;
        long threadStart = System.currentTimeMillis();
        if (!manualMemoryManagement) {
            this.thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        Set<Bytes> keySet;
                        synchronized (curated) {
                            keySet = curated.keySet();
                        }
                        long lastChecked = 0l;
                        try (Jedis j = ClassicJedisPool.getJedis()) {
                            for (Bytes bytearray : keySet) {
                                j.set(ByteFunctions.join(bytearray, DataKeySuffix.UPDATED),
                                        ByteFunctions.encodeNumber(System.currentTimeMillis()));
                            }
                            byte[] lC = j.get(ByteFunctions.join(key, DataKeySuffix.UPDATED));
                            if (lC != null) {
                                lastChecked = ByteFunctions.decodeNumber(lC);
                            }
                        }

                        long now = System.currentTimeMillis();
                        // don't run gc for the first 60 minutes to allow restarting all nodes
                        // then check if anyone has checked in the last 30s
                        if (threadStart + 3600000l < now && lastChecked + 30000l < now) {
                            long ts = now - 3600000l;
                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.set(ByteFunctions.join(key, DataKeySuffix.UPDATED), ByteFunctions.encodeNumber(now));
                                for (byte[] curatedKey : j.smembers(key.getData())) {
                                    Bytes cK = new Bytes(curatedKey);
                                    byte[] lastUpdated = j.get(ByteFunctions.join(cK, DataKeySuffix.UPDATED));
                                    if (lastUpdated == null || ByteFunctions.decodeNumber(lastUpdated) < ts) {
                                        AbstractData<?> ad = null;
                                        synchronized (AbstractData.loaded) {
                                            if (AbstractData.loaded.containsKey(cK))
                                                ad = AbstractData.loaded.get(cK);
                                        }
                                        if (ad != null) {
                                            delete(ad, true);
                                        } else {
                                            // fallback
                                            del(cK);
                                            j.srem(key.getData(), curatedKey);
                                        }
                                    }
                                }
                            }
                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.set(ByteFunctions.join(key, DataKeySuffix.UPDATED), ByteFunctions.encodeNumber(now));
                            }
                        }

                        try {
                            Thread.sleep(5000l + (long) (Math.random() * 20000));
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

            });

            thread.start();
        } else {
            this.thread = null;
        }
    }

    @SuppressWarnings("unchecked")
    private T _create(Object defaultValue) {
        if (type == null || type == DataType.INVALID || !type.canBeLoaded()) {
            return null;
        }
        Bytes k = key.append(new Bytes(getNewId(), 4));
        synchronized (AbstractData.markedAsGenerated){
            AbstractData.markedAsGenerated.add(k);
        }
        T ad;
        ad = (T) AbstractData.callConstructor(k, c);

        if (defaultValue != null) {
            if (ad instanceof SimpleInterface) {
                ((SimpleInterface<Object>) (ad)).set(defaultValue);
            }
        }
        synchronized (curated) {
            curated.put(k, ad);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(ByteFunctions.join(k, DataKeySuffix.UPDATED), ByteFunctions.encodeNumber(System.currentTimeMillis()));
            j.sadd(key.getData(), k.getData());
        }
        return ad;
    }

    /**
     * Make sure the defaultValue parameter is matching exactly the required class,
     * no implicit typecasts are performed!
     * 
     * @param defaultValue
     * @return
     */
    public T create(Object defaultValue) {
        return _create(defaultValue);
    }

    public T create() {
        return _create(null);
    }

    private long getNewId() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            return j.incr(ByteFunctions.join(key, DataKeySuffix.SPECIFIC));
        }
    }

}
