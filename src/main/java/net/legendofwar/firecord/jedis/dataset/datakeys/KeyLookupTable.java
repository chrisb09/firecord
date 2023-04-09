package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import redis.clients.jedis.Jedis;

public class KeyLookupTable {

    enum KeyLookupEntry {

        CACHE(1),
        REVERSE_CACHE(2),
        COUNTER(3);

        private byte data;

        private KeyLookupEntry(int data) {
            this.data = (byte) data;
        }

        public byte getData() {
            return data;
        }
    }

    /*
     * - 4 byte id <-> uuid
     * - 2 byte id <-> ::class entry
     * 
     */

    private byte[] key;
    private int idSize;
    private JedisLock lock;

    private HashMap<byte[], byte[]> cache = new HashMap<>(); // id, name
    private HashMap<byte[], byte[]> reverseCache = new HashMap<>(); // name, id

    public KeyLookupTable(byte[] key, int idSize) {
        this.key = key;
        this.idSize = idSize;
        lock = new JedisLock(key);
    }

    public byte[] lookUpName(long id) {
        return lookUpName(ByteFunctions.encodeId(id, idSize));
    }

    public byte[] lookUpName(byte[] id) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        lock.lock();
        try {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                byte[] cache_key = getCacheKey();
                if (j.hexists(cache_key, id)) {
                    byte[] name = j.hget(cache_key, id);
                    cache.put(id, name);
                    reverseCache.put(name, id);
                    return name;
                } else {
                    throw new InvalidParameterException(
                            "There is currently no id '" + id + "' in '" + cache_key + "'...");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public long lookUpIdLong(String name) {
        return ByteFunctions.decodeId(lookUpId(name));
    }

    public long lookUpIdLong(byte[] name) {
        return ByteFunctions.decodeId(lookUpId(name));
    }

    public byte[] lookUpId(String name) {
        return lookUpId(name.getBytes());
    }

    public byte[] lookUpId(byte[] name) {
        if (reverseCache.containsKey(name)) {
            return reverseCache.get(name);
        }
        lock.lock();
        try {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                byte[] reverseCacheKey = getReverseCacheKey();
                if (j.hexists(reverseCacheKey, name)) {
                    byte[] id = j.hget(reverseCacheKey, name);
                    cache.put(id, name);
                    reverseCache.put(name, id);
                    return id;
                } else {
                    long newId = j.incr(KeyGenerator.join(this.key, KeyLookupEntry.COUNTER.getData()));
                    byte[] newIdBytes = ByteFunctions.encodeId(newId, idSize);
                    j.hset(getCacheKey(), newIdBytes, name);
                    j.hset(getReverseCacheKey(), name, newIdBytes);
                    System.out.println("Entries for " + getCacheKey());
                    for (Map.Entry<byte[], byte[]> en : j.hgetAll(getCacheKey()).entrySet()) {
                        System.out.println(en.getKey() + ": " + en.getValue());
                    }
                    return newIdBytes;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private byte[] getCacheKey() {
        return KeyGenerator.join(this.key, KeyLookupEntry.CACHE.getData());
    }

    private byte[] getReverseCacheKey() {
        return KeyGenerator.join(this.key, KeyLookupEntry.REVERSE_CACHE.getData());
    }

    /*
     * We save & load to memory:
     * lock to coordinate access
     * 
     * We save & update on demand
     * single redis-hash (id -> name)
     * 
     * We save:
     * int counter, that specifies new entry id
     * 
     */

    // other:
    // - 1 byte id <-> entry type string
    // add to DataType

}
