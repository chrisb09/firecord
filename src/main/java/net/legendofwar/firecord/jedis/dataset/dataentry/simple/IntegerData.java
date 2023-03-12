package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;

@SuppressWarnings("unchecked")
public abstract class IntegerData<T> extends NumericData<T> {

    IntegerData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
    }

    public T incr(T value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (T) (Long) j.incr(key);
            this._update();
        }
        return this.value;
    }

    @Override
    public T add(T value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (T) (Long) j.incrBy(key, (long) value);
            this._update();
        }
        return this.value;
    }

    @Override
    public T sub(T value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (T) (Long) j.incrBy(key, -((long) (value)));
            this._update();
        }
        return this.value;
    }

}
