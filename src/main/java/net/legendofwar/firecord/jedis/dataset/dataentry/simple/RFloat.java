package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

public class RFloat extends NumericData<Float> {

    public RFloat(String key, @NotNull Float defaultValue) {
        super(key, defaultValue, DataType.FLOAT);
    }

    public RFloat(String key) {
        this(key, 0f);
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
