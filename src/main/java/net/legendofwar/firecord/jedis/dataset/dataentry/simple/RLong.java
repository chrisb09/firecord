package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public final class RLong extends NumericData<Long> {

    final static Long DEFAULT_VALUE = 0l;

    public RLong(@NotNull String key) {
        this(key, null);
    }

    public RLong(@NotNull String key, Long defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Long add(Long value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).add(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Long sub(Long value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).sub(value);
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Long mul(Long value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).mul(value);
        }
        try (AbstractData<Long> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Long.parseLong(j.get(key)) * value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Long div(Long value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).div(value);
        }
        try (AbstractData<Long> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Long.parseLong(j.get(key)) / value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Long.parseLong(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
