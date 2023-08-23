package net.legendofwar.firecord.jedis.dataset.datakeys;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
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

    private Bytes key;
    private int idSize;
    private JedisLock lock;

    private HashMap<Bytes, Bytes> cache = new HashMap<>(); // id, name
    private HashMap<Bytes, Bytes> reverseCache = new HashMap<>(); // name, id

    public KeyLookupTable(Bytes key, int idSize) {
        this.key = key;
        this.idSize = idSize;
        lock = new JedisLock(key);
    }

    public Bytes lookUpName(long id) {
        return lookUpName(new Bytes(id, idSize));
    }

    public Bytes lookUpName(Bytes id) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        lock.lock();
        try {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                byte[] cache_key = getCacheKey();
                if (j.hexists(cache_key, id.getData())) {
                    Bytes name = new Bytes(j.hget(cache_key, id.getData()));
                    cache.put(id, name);
                    reverseCache.put(name, id);
                    return name;
                } else {
                    throw new InvalidParameterException(
                            "There is currently no id '" + id + "' in '" + ByteFunctions.asHexadecimal(cache_key) + "'...");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public long lookUpIdLong(String name) {
        return lookUpId(name).decodeNumber();
    }

    public long lookUpIdLong(Bytes name) {
        return lookUpId(name).decodeNumber();
    }

    public Bytes lookUpId(byte[] name){
        return lookUpId(new Bytes(name));
    }

    public Bytes lookUpId(String name) {
        return lookUpId(name.getBytes());
    }

    public Bytes lookUpId(Bytes name) {
        if (reverseCache.containsKey(name)) {
            return reverseCache.get(name);
        }
        lock.lock();
        try {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                byte[] reverseCacheKey = getReverseCacheKey();
                if (j.hexists(reverseCacheKey, name.getData())) {
                    Bytes id = new Bytes(j.hget(reverseCacheKey, name.getData()));
                    cache.put(id, name);
                    reverseCache.put(name, id);
                    return id;
                } else {
                    long newId = j.incr(this.key.append(KeyLookupEntry.COUNTER.getData()).getData());
                    Bytes newIdBytes = new Bytes(newId, (byte) idSize);
                    j.hset(getCacheKey(), newIdBytes.getData(), name.getData());
                    j.hset(getReverseCacheKey(), name.getData(), newIdBytes.getData());
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
        return this.key.append(KeyLookupEntry.CACHE.getData()).getData();
    }

    private byte[] getReverseCacheKey() {
        return ByteFunctions.join(this.key, KeyLookupEntry.REVERSE_CACHE.getData());
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
