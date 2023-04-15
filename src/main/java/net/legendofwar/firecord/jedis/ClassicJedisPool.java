package net.legendofwar.firecord.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.tool.ReadProperties;

public class ClassicJedisPool {

    private static Object staticLock = new Object();
    private static JedisPool pool = null;
    static HashMap<Jedis, String> last_requested_by = new HashMap<Jedis, String>();

    public static CustomJedisPoolConfig<Jedis> buildPoolConfig() {
        return new CustomJedisPoolConfig<Jedis>();
    }

    public static void destroy() {
        if (pool != null) {
            pool.close();
        }
        pool = null;
    }

    private static void createPool() {

        String[] properties = null;
        try {
            properties = ReadProperties.readProperties("data/.secret/redis",
                    new String[] { "host", "port", "password" });
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (properties != null) {
            System.out.println("Create jedis pool...");
            System.out.println("Host: "+properties[0]);
            System.out.println("Port: "+properties[1]);
            System.out.println("Password: "+properties[2].subSequence(0, 2)+"...");
            GenericObjectPoolConfig<Jedis> config = buildPoolConfig();
            pool = new JedisPool(config, properties[0], Integer.parseInt(properties[1]), 3000, properties[2]);
        }

    }

    /*
     * Returning a redis instance to the pool is now done with .close()
     * Make sure to use try{}finally{j.close()} or something similar to prevent leaks
     */

    public static Jedis getJedis() {
        if (pool == null) {
            synchronized(staticLock) {
                if (pool == null) { // repeat in sync
                    createPool();
                }
            }
        }
        Jedis res = pool.getResource();
        try{
            String caller = Thread.currentThread().getStackTrace()[2].getClassName()+"."
                +Thread.currentThread().getStackTrace()[2].getMethodName()+" in "
                +Thread.currentThread().getStackTrace()[2].getFileName();
            last_requested_by.put(res, caller);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JedisWrapper(res);
    }

    public static HashMap<String, Integer> getRessourceOwner() {
        HashMap<String,Integer> used = new HashMap<String,Integer>();
        for (Map.Entry<Jedis, String> entry : last_requested_by.entrySet()) {
            if (!used.containsKey(entry.getValue()))
                used.put(entry.getValue(), 0);
            used.put(entry.getValue(), used.get(entry.getValue())+1);
        }
        return used;
    }

    public static String getPoolCurrentUsage()
    {

        if (pool == null){
            return "not initiated.";
        }

        CustomJedisPoolConfig<Jedis> poolConfig = buildPoolConfig();

        int active = pool.getNumActive();
        int idle = pool.getNumIdle();
        int total = active + idle;
        String log = String.format(
                "JedisPool: Active=%d, Idle=%d, Waiters=%d, total=%d, maxTotal=%d, minIdle=%d, maxIdle=%d",
                active,
                idle,
                pool.getNumWaiters(),
                total,
                poolConfig.getMaxTotal(),
                poolConfig.getMinIdle(),
                poolConfig.getMaxIdle()
        );

        return log;
    }

    /**
     * Shortcut function to get a single value
     * @return redis get at key result
     */
    public static byte[] getValue(byte[] key){
        if (key == null) {
            return null;
        }
        try (Jedis j = getJedis()){
            return j.get(key);
        }
    }

    public static Bytes getValue(Bytes key){
        if (key == null) {
            return null;
        }
        return new Bytes(getValue(key.getData()));
    }

}
