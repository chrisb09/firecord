package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import redis.clients.jedis.Jedis;

public final class RLong extends NumericData<Long> {

    final static DataGenerator<RLong> GENERATOR = new DataGenerator<>(new Bytes("rlong"), RLong.class);

    final static RLong create() {
        return GENERATOR.create();
    }

    final static RLong create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RLong object) {
        DataGenerator.delete(object);
    }

    final static Long DEFAULT_VALUE = 0l;

    public RLong(@NotNull Bytes key) {
        this(key, null);
    }

    public RLong(@NotNull Bytes key, Long defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Long add(Long value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Long oldValue = this.value;
            this.value = j.incrBy(key.getData(), value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Long sub(Long value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Long oldValue = this.value;
            this.value = j.incrBy(key.getData(), -value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Long mul(Long value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Long oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Long.parseLong(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Long div(Long value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Long oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Long.parseLong(new Bytes(j.get(key.getData())).asString()) / value;
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
        this.value = Long.parseLong(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
