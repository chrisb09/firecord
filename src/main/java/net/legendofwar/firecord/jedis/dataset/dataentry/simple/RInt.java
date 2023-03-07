package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import redis.clients.jedis.Jedis;

public class RInt extends NumericData<Integer> {

    public RInt(String key, @NotNull Integer defaultValue) {
        super(key, defaultValue);
    }

    public RInt(String key) {
        this(key, 0);
    }

    @Override
    public Integer add(Integer value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (int) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer sub(Integer value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (int) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer mul(Integer value) {
        try (SimpleData<Integer> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(j.get(key)) * value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Integer div(Integer value) {
        try (SimpleData<Integer> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(j.get(key)) / value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Integer.parseInt(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
