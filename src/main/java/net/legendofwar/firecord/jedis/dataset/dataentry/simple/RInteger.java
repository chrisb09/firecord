package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import redis.clients.jedis.Jedis;

public final class RInteger extends NumericData<Integer> {

    final static DataGenerator<RInteger> GENERATOR = new DataGenerator<>(new Bytes("rinteger"), RInteger.class);

    final static RInteger create() {
        return GENERATOR.create();
    }

    final static RInteger create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RInteger object) {
        DataGenerator.delete(object);
    }

    final static Integer DEFAULT_VALUE = 0;

    public RInteger(@NotNull Bytes key) {
        this(key, null);
    }

    public RInteger(@NotNull Bytes key, Integer defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Integer add(Integer value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Integer oldValue = this.value;
            this.value = (Integer) (int) j.incrBy(key.getData(), value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Integer sub(Integer value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        // single redis commands are atomic, therefore we don't need a lock
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Integer oldValue = this.value;
            this.value = (Integer) (int) j.incrBy(key.getData(), -value);
            this._update(oldValue);
        }
        return this.value;
    }

    @Override
    public Integer mul(Integer value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Integer oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(new Bytes(j.get(key.getData())).asString()) * value;
                j.set(key.getData(), new Bytes(this.value.toString()).getData());
            }
        }
        this._update(oldValue);
        return this.value;
    }

    @Override
    public Integer div(Integer value) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        Integer oldValue = this.value;
        try (JedisLock lock = lock()) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.value = Integer.parseInt(new Bytes(j.get(key.getData())).asString()) / value;
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
        this.value = Integer.parseInt(new String(value));
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    public double getSortScore(){
        return this.value;
    }

}
