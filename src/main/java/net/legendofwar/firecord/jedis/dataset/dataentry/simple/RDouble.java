package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;

public class RDouble extends NumericData<Double> {

    public RDouble(String key, @NotNull Double defaultValue) {
        super(key, defaultValue, DataType.DOUBLE);
    }

    public RDouble(String key) {
        this(key, 0d);
    }

    @Override
    public Double add(Double value) {
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
