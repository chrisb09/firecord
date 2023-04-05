package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public final class RByte extends NumericData<Byte> {

    final static Byte DEFAULT_VALUE = 0;

    public RByte(@NotNull String key) {
        this(key, null);
    }

    public RByte(@NotNull String key, Byte defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Byte add(Byte value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).add(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (byte) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Byte sub(Byte value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).sub(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (byte) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Byte mul(Byte value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).mul(value);
        }
        try (AbstractData<Byte> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(j.get(key)) * value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Byte div(Byte value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).div(value);
        }
        try (AbstractData<Byte> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(j.get(key)) / value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Byte.parseByte(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
