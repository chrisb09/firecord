package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import redis.clients.jedis.Jedis;

public final class RByte extends NumericData<Byte> {

    /*
     * For redis, these entries are "strings", hence to use
     * the incr() and incrBy() functions we need to save
     * all integer values as strings which can be parsed to
     * a base_10 64-bit integer
     */

    final static Byte DEFAULT_VALUE = 0;

    public RByte(@NotNull Bytes key) {
        this(key, null);
    }

    public RByte(@NotNull Bytes key, Byte defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Byte add(Byte value){
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Byte oldValue = this.value;
            this.value = (byte) j.incrBy(key.getData(), value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Byte sub(Byte value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Byte oldValue = this.value;
            this.value = (byte) j.incrBy(key.getData(), -value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Byte mul(Byte value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Byte oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(new Bytes(j.get(key.getData())).asString()) * value);
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Byte div(Byte value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Byte oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(new Bytes(j.get(key.getData())).asString()) / value);
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value.toString());
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = Byte.parseByte(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
