package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public class RInteger extends NumericData<Integer> {

    final static Integer DEFAULT_VALUE = 0;

    public RInteger(@NotNull Bytes key) {
        this(key, null);
    }

    public RInteger(@NotNull Bytes key, Integer defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Integer add(Integer value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).add(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (Integer) (int) j.incrBy(key.getData(), value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer sub(Integer value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).sub(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (Integer) (int) j.incrBy(key.getData(), -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer mul(Integer value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).mul(value);
        }
        try (AbstractData<Integer> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Integer div(Integer value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).div(value);
        }
        try (AbstractData<Integer> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(new Bytes(j.get(key.getData())).asString()) / value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value.toString());
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = Integer.parseInt(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
