package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public final class RWrapper extends SmallData<AbstractData<?>> {

    final static Object DEFAULT_VALUE = null;

    final static AbstractData<?> NULL_ENTRY = new RString("null");

    /**
     * If the wrapper entry does not exist, the default value cannot act as a
     * substitute!
     * 
     * @param key
     * @param objectKey
     */

    public RWrapper(String key, @NotNull String objectKey) {
        super(key);
        this.set(AbstractData.create(objectKey));
    }

    public RWrapper(String key) {
        this(key, ClassicJedisPool.getValue(key));
    }

    @Override
    protected void fromString(String value) {
        AbstractData<?> entry = AbstractData.create(value);
        if (entry != null) {
            this.value = entry;
        }
    }

    @Override
    public String toString() {
        if (this.value != null) {
            return this.value.getKey();
        } else {
            return null;
        }
    }

    public void set(String key) {
        fromString(key);
        this.set(this.value);
    }

    public void setIfEmpty(String key) {
        if (this.value == null) {
            set(key);
        }
    }

}
