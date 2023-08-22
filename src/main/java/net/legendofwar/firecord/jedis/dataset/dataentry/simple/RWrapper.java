package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public final class RWrapper extends SmallData<AbstractData<?>> {

    final static Object DEFAULT_VALUE = null;

    final static AbstractData<?> NULL_ENTRY = new RString(new Bytes());

    /*
     * object or the corresponding objeyKey cannot be null !
     */

    public RWrapper(@NotNull Bytes key) {
        this(key, ClassicJedisPool.getValue(key));
    }

    public RWrapper(@NotNull Bytes key, @NotNull Bytes objectKey) {
        this(key, AbstractData.create(objectKey));
    }

    public RWrapper(@NotNull Bytes key, @NotNull AbstractData<?> object) {
        super(key, object);
    }

    @Override
    protected Bytes toBytes() {
        return this.value.getKey();
    }

    @Override
    protected void fromBytes(byte[] value) {
        AbstractData<?> entry = AbstractData.create(new Bytes(value));
        if (entry != null) {
            this.value = entry;
        }
    }

    @Override
    public String toString() {
        if (this.value != null) {
            return "[" + this.value.getKey() + "]";
        } else {
            return "[null]";
        }
    }

    public void set(Bytes key) {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        fromBytes(key);
        this.set(this.value);
    }

    public void setIfEmpty(Bytes key) {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        if (this.value == null) {
            set(key);
        }
    }

}
