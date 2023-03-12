package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import redis.clients.jedis.Jedis;

public class DataPool<T extends AbstractData<?>> {

    static DataPool<AbstractData<?>> abstractDataPool = new DataPool<>("datapool");

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

    final String key;
    final Thread thread;

    Map<String, T> curated = new HashMap<String, T>();

    public DataPool(String key) {
        this.key = key;
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

                    if (lastChecked + 10000 < System.currentTimeMillis()) {
                        long ts = System.currentTimeMillis() - 30000;
                        try (Jedis j = ClassicJedisPool.getJedis()) {
                            j.set(key + ":updated", System.currentTimeMillis() + "");
                            for (String s : j.smembers(key)) {
                                String v = j.get(s + ":updated");
                                if (v == null || Long.parseLong(v) < ts) {
                                    del(s);
                                    j.srem(key, s);
                                }
                            }
                        }
                    }

                    try {
                        Thread.sleep(5000l + (long) (Math.random() * 10000));
                    } catch (InterruptedException e) {
                        // does not matter
                    }
                }
            }

        });

        thread.start();
    }

    @SuppressWarnings("unchecked")
    private T _create(DataType dt, Object defaultValue) {
        String k = key + ":" + getNewId();
        T ad;
        if (defaultValue == null) {
            ad = (T) AbstractData.callConstructor(k, dt.getC());
        } else {
            ad = (T) AbstractData.callConstructor(k, dt.getC(), defaultValue);
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

    @SuppressWarnings("unchecked")
    public void delete(T ad) {
        boolean contained = false;
        synchronized (curated) {
            contained = (curated.containsKey(ad.getKey()));
            curated.remove(ad.getKey());
        }
        if (contained) {
            del(ad.getKey());
        }
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
                }
            }
            c = c.getSuperclass();
        }
    }

    private void del(String k) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.del(k + ":updated", k + ":class", k + ":type", k);
        }
    }

    private long getNewId() {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            return j.incr(key + ":id");
        }
    }

}
