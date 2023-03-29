package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public class DataGenerator<T extends AbstractData<?>> {

    static List<DataGenerator<?>> dataPools = new ArrayList<>();

    static DataGenerator<AbstractData<?>> abstractDataPool = new DataGenerator<>("datapool");

    /**
     * Creates an anonymous type, meaning we don't have to specify a key. Use with
     * care as inproper use can cause memory leaks and a high garbage collector
     * workload.
     * 
     * @param dt Specifies the type of the created entry
     * @return
     */
    public static AbstractData<?> createAnonymous(DataType dt) {
        return abstractDataPool.create(dt);
    }

    /**
     * Make sure the defaultValue class matches exactly the class required in the
     * constructor of the corresponding abstractdata class
     * 
     * @param dt
     * @param defaultValue
     * @return
     */
    public static AbstractData<?> createAnonymous(DataType dt, Object defaultValue) {
        return abstractDataPool.create(dt, defaultValue);
    }

    @SuppressWarnings("unchecked")
    static void delete(AbstractData<?> ad, boolean deleteInDB){
        // remove from pools curated list
        synchronized(dataPools){
            for (DataGenerator<?> dp : dataPools){
                synchronized (dp.curated) {
                    dp.curated.remove(ad.getKey());
                    try (Jedis j = Firecord.getJedis()) {
                        j.srem(dp.key, ad.getKey());
                    }
                }
                
            }
        }
        // actually delete entries in DB
        if (deleteInDB) {
            del(ad.getKey());
            // send firecord message to other nodes
            Firecord.broadcast("del_key", ad.getKey());
        }

        // Delete
        Class<?> c = ad.getClass();
        while (c != AbstractData.class) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (field.getName().equals("loaded")) {
                        field.setAccessible(true);
                        try {
                            HashMap<String, ?> loaded = (HashMap<String, ?>) (field.get(null));
                            synchronized (loaded) {
                                loaded.remove(ad.getKey());
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (AbstractObject.class.isAssignableFrom(c)){
                    if (SimpleInterface.class.isAssignableFrom(field.getType())){
                        AbstractData<?> si = null;
                        try {
                            si = (AbstractData<?>) field.get(ad);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if (si != null){
                            delete(si, deleteInDB);
                        }
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    public static void delete(AbstractData<?> ad) {
        delete(ad, true);
    }

    private static void del(String k) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(k + ":updated", k + ":class", k + ":type", k);
        }
    }

    final String key;
    final Thread thread;

    Map<String, T> curated = new HashMap<String, T>();

    public DataGenerator(String key){
        this(key, true);
    }

    /**
     * 
     * @param key
     * @param manualMemoryManagement true means there will be no automatic deletion
     */

    public DataGenerator(String key, boolean manualMemoryManagement) {
        synchronized(dataPools){
            dataPools.add(this);
        }
        this.key = key;
        long threadStart = System.currentTimeMillis();
        if (manualMemoryManagement) {
            this.thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        Set<String> keySet;
                        synchronized (curated) {
                            keySet = curated.keySet();
                        }
                        long lastChecked = 0l;
                        try (Jedis j = ClassicJedisPool.getJedis()) {
                            for (String s : keySet) {
                                j.set(s + ":updated", System.currentTimeMillis() + "");
                            }
                            String lC = j.get(key + ":updated");
                            if (lC != null) {
                                lastChecked = Long.parseLong(lC);
                            }
                        }

                        long now = System.currentTimeMillis();
                        // don't run gc for the first 60 minutes to allow restarting all nodes
                        // then check if anyone has checked in the last 30s
                        if (threadStart + 3600000l < now && lastChecked + 30000l < now) {
                            long ts = now - 3600000l;
                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.set(key + ":updated", now + "");
                                for (String s : j.smembers(key)) {
                                    String v = j.get(s + ":updated");
                                    if (v == null || Long.parseLong(v) < ts) {
                                        AbstractData<?> ad = null;
                                        synchronized (AbstractData.loaded){
                                            if (AbstractData.loaded.containsKey(s))
                                                ad = AbstractData.loaded.get(s);
                                        }
                                        if (ad != null){
                                            delete(ad, true);
                                        } else {
                                            // fallback
                                            del(s);
                                            j.srem(key, s);
                                        }
                                    }
                                }
                            }
                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.set(key + ":updated", now + "");
                            }
                        }

                        try {
                            Thread.sleep(5000l + (long) (Math.random() * 20000));
                        } catch (InterruptedException e) {
                            // does not matter
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
    private T _create(DataType dt, Object defaultValue) {
        String k = key + ":" + getNewId();
        T ad;
        if (!dt.canBeLoaded()) {
            return null;
        }
            ad = (T) AbstractData.callConstructor(k, dt.getC());
        
        if (defaultValue != null) {
            if (ad instanceof SimpleInterface) {
                ((SimpleInterface<Object>) (ad)).set(defaultValue);
            }
        }
        synchronized (curated) {
            curated.put(k, ad);
        }
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(k + ":updated", System.currentTimeMillis() + "");
            j.sadd(key, k);
        }
        return ad;
    }

    /**
     * Make sure the defaultValue parameter is matching exactly the required class,
     * no implicit typecasts are performed!
     * 
     * @param dt
     * @param defaultValue
     * @return
     */
    public T create(DataType dt, Object defaultValue) {
        return _create(dt, defaultValue);
    }

    public T create(DataType dt) {
        return _create(dt, null);
    }

    private long getNewId() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            return j.incr(key + ":id");
        }
    }

}
