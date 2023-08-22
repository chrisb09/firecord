package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import redis.clients.jedis.Jedis;

public final class RShort extends NumericData<Short> {

    final static Short DEFAULT_VALUE = 0;

    public RShort(@NotNull Bytes key) {
        this(key, null);
    }

    public RShort(@NotNull Bytes key, Short defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Short add(Short value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key.getData(), value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short sub(Short value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            this.value = (short) j.incrBy(key.getData(), -value);
            this._update();
        }
        return this.value;
    }

    @Override
    public Short mul(Short value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Short> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (short) (Short.parseShort(new Bytes(j.get(key.getData())).asString()) * value);
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
                this._update();
            }
        }
        return this.value;
    }

    @Override
    public Short div(Short value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        try (AbstractData<Short> l = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = (short) (Short.parseShort(new Bytes(j.get(key.getData())).asString()) / value);
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
        this.value = Short.parseShort(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
