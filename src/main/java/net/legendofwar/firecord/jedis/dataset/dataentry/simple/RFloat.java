package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import redis.clients.jedis.Jedis;

public final class RFloat extends NumericData<Float> {

    final static Float DEFAULT_VALUE = 0.0f;

    public RFloat(@NotNull Bytes key) {
        this(key, null);
    }

    public RFloat(@NotNull Bytes key, Float defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Float add(Float value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Float oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(new Bytes(j.get(key.getData())).asString()) + value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Float sub(Float value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Float oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(new Bytes(j.get(key.getData())).asString()) - value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Float mul(Float value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Float oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Float div(Float value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Float oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(new Bytes(j.get(key.getData())).asString()) / value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(value.toString());
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = Float.parseFloat(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
