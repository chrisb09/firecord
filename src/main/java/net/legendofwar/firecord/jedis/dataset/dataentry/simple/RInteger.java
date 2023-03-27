package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public class RInteger extends NumericData<Integer> {

    final static Integer DEFAULT_VALUE = 0;

    public RInteger(String key) {
        super(key);
    }

    @Override
    public Integer add(Integer value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (Integer) (int) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer sub(Integer value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (Integer) (int) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Integer mul(Integer value) {
        try (AbstractData<Integer> l = lock()) {
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
        try (AbstractData<Integer> l = lock()) {
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
