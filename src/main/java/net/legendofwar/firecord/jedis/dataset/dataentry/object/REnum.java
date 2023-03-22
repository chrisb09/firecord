package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;

@SuppressWarnings("unchecked")
public class REnum<T extends Enum<T>> extends AbstractObject {

    Class<T> c;
    RString className;
    RString value;

    public REnum(String key, @NotNull T defaultValue) {
        super(key);
        c = (Class<T>) defaultValue.getClass();
        className.set(defaultValue.getClass().getName());
        value.setIfNull(defaultValue.name());
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
    }

    @Override
    public String toString() {
        return value.get();
    }

    public T get() {
        return Enum.valueOf(c, this.value.get());
    }

    public void set(T value) {
        this.value.set(value.name());
    }

}
