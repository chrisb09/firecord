package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

public class RLong extends IntegerData<Long> {

    public RLong(String key, @NotNull Long defaultValue) {
        super(key, defaultValue, DataType.LONG);
    }

    public RLong(String key) {
        this(key, 0L);
    }

    @Override
    public Long add(Long value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Long sub(Long value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Long mul(Long value) {
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
