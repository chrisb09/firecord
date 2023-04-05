package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;

@SuppressWarnings("unchecked")
public final class REnum<T extends Enum<T>> extends SimpleAbstractObject<T> {

    final static Enum<?> DEFAULT_VALUE = null;

    Class<T> c;
    RString className;
    RString value;

    public REnum(@NotNull String key, @NotNull T defaultValue) {
        super(key);
        c = (Class<T>) defaultValue.getClass();
        className.set(defaultValue.getClass().getName());
        value.setIfEmpty(defaultValue.name());
        if (value.get().length() == 0) {
            value.set(defaultValue.name());
        }
    }

    public REnum(String key) {
        super(key);
        Class<?> c = null;
        try {
            String cN = className.get();
            if (cN != null && cN.length() != 0) {
                c = Class.forName(cN);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (c != null) {
            this.c = (Class<T>) c;
        } else {
            this.c = null;
        }
    }

    void setC(Class<T> c) {
        this.c = c;
        this.className.set(c.getName());
    }

    @Override
    public String toString() {
        return value.get();
    }

    public T get() {
        return Enum.valueOf(c, this.value.get());
    }

    public boolean set(T value) {
        return this.value.set(value.name());
    }

    @Override
    public boolean setIfEmpty(T defaultValue) {
        if (this.value.get() == null) {
            return this.set(defaultValue);
        }
        return false;
    }

    @Override
    public void listen(Consumer<SimpleInterface<T>> listener) {
        this.value.listen(value -> listener.accept(this));
    }

    // should NOT care about being a temp value
    @Override
    public T getTempValue() {
        if (this.key != null) {
            throw new UnsupportedOperationException(
                    "This method is only supposed to be used for temporary entries (key=null) internally." +
                            "Please use the normal .get() method for regular entries.");
        }
        return Enum.valueOf(c, this.value.get());
    }

}
