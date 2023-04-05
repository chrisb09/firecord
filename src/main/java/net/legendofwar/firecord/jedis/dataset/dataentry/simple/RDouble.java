package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import redis.clients.jedis.Jedis;

public final class RDouble extends NumericData<Double> {

    final static Double DEFAULT_VALUE = 0.0;

    public RDouble(@NotNull String key) {
        this(key, null);
    }

    public RDouble(@NotNull String key, Double defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Double add(Double value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).add(value);
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(j.get(key)) + value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double sub(Double value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).sub(value);
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(j.get(key)) - value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double mul(Double value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).mul(value);
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(j.get(key)) * value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double div(Double value) {
        if (this.key == null) {
            // only abstract objects should create temporary entries
            return AbstractObject.replaceTemp(this).div(value);
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(j.get(key)) / value;
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Double.parseDouble(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
