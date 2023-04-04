package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public final class RFloat extends NumericData<Float> {

    final static Float DEFAULT_VALUE = 0.0f;

    public RFloat(String key) {
        this(key, null);
    }

    public RFloat(String key, Float defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Float add(Float value) {
        try (AbstractData<Float> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(j.get(key)) + value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Float sub(Float value) {
        try (AbstractData<Float> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(j.get(key)) - value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Float mul(Float value) {
        try (AbstractData<Float> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(j.get(key)) * value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Float div(Float value) {
        try (AbstractData<Float> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Float.parseFloat(j.get(key)) / value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Float.parseFloat(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
