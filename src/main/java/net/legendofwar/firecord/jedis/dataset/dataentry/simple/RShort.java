package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public final class RShort extends NumericData<Short> {

    final static Short DEFAULT_VALUE = 0;

    public RShort(String key) {
        super(key);
    }

    @Override
    public Short add(Short value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short sub(Short value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short mul(Short value) {
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
