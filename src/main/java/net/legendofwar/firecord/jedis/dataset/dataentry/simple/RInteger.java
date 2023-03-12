package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import redis.clients.jedis.Jedis;

public class RInteger extends IntegerData<Integer> {

    public RInteger(String key, @NotNull Integer defaultValue) {
        super(key, defaultValue, DataType.INTEGER);
    }

    public RInteger(String key) {
        this(key, 0);
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
