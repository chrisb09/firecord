package net.legendofwar.firecord.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisAccessControlException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.tool.ReadProperties;

public class ClassicJedisPool {

    private static Object staticLock = new Object();
    private static List<JedisPool> pools = new ArrayList<>();
    private static List<Boolean> created = new ArrayList<>();
    private static List<Boolean> closed = new ArrayList<>();
    static HashMap<Jedis, String> last_requested_by = new HashMap<Jedis, String>();

    public static CustomJedisPoolConfig<Jedis> buildPoolConfig() {
        return new CustomJedisPoolConfig<Jedis>();
    }

    public static boolean any(Iterable<Boolean> list) {
        for (boolean b : list) {
            if (b) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesPoolExist() {
        return pools.size() > 0 && any(pools.stream().map(pool -> pool != null).toList());
    }

    public static boolean doesPoolExist(int database) {
        return pools.size() > database && pools.get(database) != null;
    }

    public static void destroy() {
        if (doesPoolExist()) {
            System.out.println("Closing Pools...");
            for (JedisPool pool : pools) {
                while (closed.size() <= pools.indexOf(pool)){
                    closed.add(false);
                }
                closed.set(pools.indexOf(pool),true);
                if (pool != null) {
                    pool.close();
                    int counter = 0;
                    while (!pool.isClosed() && ++counter < 150) {
                        System.out.println("...not fully closed yet, waiting 0.1s...");
                        try {
                            Thread.sleep(100l);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (counter == 150) {
                        System.out.println("Waiting stopped prematurely.");
                    }
                    System.out.println("Open connections: " + pool.getNumActive());
                }
            }
        }
        pools.clear();
    }

    private static long createUser(String host, String port, String username, String password) {
        // Connect to Redis (assuming local server with no password)
        try (Jedis jedis = new Jedis(host, Integer.parseInt(port), 3000)) {
            if (jedis.auth(password).equalsIgnoreCase("OK")) {
                // Check if user exists
                List<String> userList = jedis.aclList();
                boolean userExists = userList.stream().anyMatch(u -> u.contains("user " + username + " "));

                // If user doesn't exist, create it
                jedis.aclSetUser(username, "on", ">" + password, "+@all", "&*", "~*");
                if (!userExists) {
                    System.out.println("User '" + username + "' created.");
                } else {
                    System.out.println("User '" + username + "' already exists.");
                }

                // Use the user for further operations
                try (Jedis userJedis = new Jedis(host, Integer.parseInt(port), 3000)) {
                    userJedis.auth(username, password);

                    // Test a command
                    userJedis.set("test", "okay");
                    System.out.println("Value set by '" + username + "': " + userJedis.get("test"));
                    return 1l;
                } catch (JedisAccessControlException e) {
                    System.err.println("Permission denied: " + e.getMessage());
                }
            }
        }
        return 0;
    }

    public static boolean isConnected() {
        return isConnected(0);
    }

    public static boolean isConnected(int database) {
        return created.size() > database && created.get(database) && (closed.size() <= database || !closed.get(database)) && doesPoolExist(database) && !pools.get(database).isClosed();
    }

    private static void createPool(int database) {

        String[] properties = null;
        try {
            properties = ReadProperties.readProperties("data/.secret/redis",
                    new String[] { "host", "port", "password" });
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (properties != null) {
            System.out.println("Create jedis pool...");
            System.out.println("Host: " + properties[0]);
            System.out.println("Port: " + properties[1]);
            System.out.println("Password: " + properties[2].subSequence(0, 2) + "...");
            GenericObjectPoolConfig<Jedis> config = buildPoolConfig();
            System.out.println("Creating pool...");
            long userId = createUser(properties[0], properties[1], Firecord.getIdName(), properties[2]);
            JedisPool pool;
            if (userId == 0) {
                pool = new JedisPool(config, properties[0], Integer.parseInt(properties[1]), 3000, properties[2],
                        database);
            } else {
                pool = new JedisPool(config, properties[0], Integer.parseInt(properties[1]), 3000,
                        Firecord.getIdName(),
                        properties[2], database);
            }
            while (pools.size() <= database) {
                pools.add(null);
            }
            pools.set(database, pool);
        }

    }

    public static Jedis getJedis() {
        return getJedis(0);
    }

    /*
     * Returning a redis instance to the pool is now done with .close()
     * Make sure to use try{}finally{j.close()} or something similar to prevent
     * leaks
     */

    public static Jedis getJedis(int database) {
        if (!doesPoolExist(database)) {
            synchronized (staticLock) {
                if (created.size() <= database || !created.get(database)) { // repeat in sync
                    while (created.size() <= database){
                        created.add(false);
                    }
                    created.set(database, true);
                    createPool(database);
                } else {
                    // if the pool has been created once but is null now then it got destroyed
                    // already, meaning we're shutting down
                    return null;
                }
            }
        }
        Jedis res = pools.get(database).getResource();
        try {
            String caller = Thread.currentThread().getStackTrace()[2].getClassName() + "."
                    + Thread.currentThread().getStackTrace()[2].getMethodName() + " in "
                    + Thread.currentThread().getStackTrace()[2].getFileName();
            last_requested_by.put(res, caller);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static HashMap<String, Integer> getRessourceOwner() {
        return getRessourceOwner(0);
    }

    public static HashMap<String, Integer> getRessourceOwner(int database) {
        HashMap<String, Integer> used = new HashMap<String, Integer>();
        HashSet<Jedis> idleJedis = new HashSet<>();
        for (int i = 0; i < getIdleClients(); i++) {
            idleJedis.add(pools.get(database).getResource());
        }
        try {
            for (Map.Entry<Jedis, String> entry : last_requested_by.entrySet()) {
                if (!idleJedis.contains(entry.getKey())) {
                    if (!used.containsKey(entry.getValue()))
                        used.put(entry.getValue(), 0);
                    used.put(entry.getValue(), used.get(entry.getValue()) + 1);
                }
            }
        } finally {
            for (Jedis j : idleJedis) {
                j.close();
            }
        }
        return used;
    }

    public static int getIdleClients() {
        return getIdleClients(0);
    }

    public static int getIdleClients(int database) {
        if (!doesPoolExist(database)) {
            return 0;
        }
        return pools.get(database).getNumIdle();
    }

    public static String getPoolCurrentUsage() {
        return getPoolCurrentUsage(0);
    }

    public static String getPoolCurrentUsage(int database) {

        if (!doesPoolExist(database)) {
            return "not initiated.";
        }

        CustomJedisPoolConfig<Jedis> poolConfig = buildPoolConfig();

        int active = pools.get(database).getNumActive();
        int idle = pools.get(database).getNumIdle();
        int total = active + idle;
        String log = String.format(
                "JedisPool: Active=%d, Idle=%d, Waiters=%d, total=%d, maxTotal=%d, minIdle=%d, maxIdle=%d",
                active,
                idle,
                pools.get(database).getNumWaiters(),
                total,
                poolConfig.getMaxTotal(),
                poolConfig.getMinIdle(),
                poolConfig.getMaxIdle());

        return log;
    }

    /**
     * Shortcut function to get a single value
     * 
     * @return redis get at key result
     */
    public static byte[] getValue(byte[] key) {
        if (key == null) {
            return null;
        }
        try (Jedis j = getJedis()) {
            return j.get(key);
        }
    }

    public static Bytes getValue(Bytes key) {
        if (key == null) {
            return null;
        }
        return new Bytes(getValue(key.getData()));
    }

}
