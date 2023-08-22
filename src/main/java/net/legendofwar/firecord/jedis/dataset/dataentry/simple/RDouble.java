package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public final class RDouble extends NumericData<Double> {

    final static Double DEFAULT_VALUE = 0.0;

    public RDouble(@NotNull Bytes key) {
        this(key, null);
    }

    public RDouble(@NotNull Bytes key, Double defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Double add(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) + value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double sub(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) - value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double mul(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Double div(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Double> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) / value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value.toString());
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = Double.parseDouble(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
