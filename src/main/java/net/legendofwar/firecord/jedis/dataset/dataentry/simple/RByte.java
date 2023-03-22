package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

public class RByte extends IntegerData<Byte> {

    public RByte(String key, @NotNull Byte defaultValue) {
        super(key, defaultValue, DataType.BYTE);
    }

    public RByte(String key) {
        this(key, (byte) 0);
    }

    @Override
    public Byte add(Byte value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (byte) j.incrBy(key, value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Byte sub(Byte value) {
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (byte) j.incrBy(key, -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Byte mul(Byte value) {
        try (AbstractData<Byte> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(j.get(key)) * value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Byte div(Byte value) {
        try (AbstractData<Byte> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (byte) (Byte.parseByte(j.get(key)) / value);
                j.set(key, this.value.toString());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    protected void fromString(String value) {
        this.value = Byte.parseByte(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
