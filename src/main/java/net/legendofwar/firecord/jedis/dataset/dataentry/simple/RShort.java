package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public final class RShort extends NumericData<Short> {

    final static Short DEFAULT_VALUE = 0;

    public RShort(@NotNull String key) {
        this(key, null);
    }

    public RShort(@NotNull String key, Short defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Short add(Short value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).add(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short sub(Short value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).sub(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short mul(Short value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).mul(value);
        }
        try (AbstractData<Short> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (short) (Short.parseShort(j.get(key)) * value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Short div(Short value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).div(value);
        }
        try (AbstractData<Short> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (short) (Short.parseShort(j.get(key)) / value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Short.parseShort(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
