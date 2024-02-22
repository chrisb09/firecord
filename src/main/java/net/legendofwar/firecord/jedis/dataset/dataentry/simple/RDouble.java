package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import redis.clients.jedis.Jedis;

public final class RDouble extends NumericData<Double> {

    final static DataGenerator<RDouble> GENERATOR = new DataGenerator<>(new Bytes("rdouble"), RDouble.class);

    final static RDouble create() {
        return GENERATOR.create();
    }

    final static RDouble create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RDouble object) {
        DataGenerator.delete(object);
    }

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
        Double oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) + value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Double sub(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Double oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) - value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Double mul(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Double oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Double div(Double value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Double oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Double.parseDouble(new Bytes(j.get(key.getData())).asString()) / value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
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
