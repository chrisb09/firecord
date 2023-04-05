package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class RJson extends LargeData<JSONObject> {

    final static JSONObject DEFAULT_VALUE = new JSONObject();

    public RJson(@NotNull String key) {
        this(key, null);
    }

    public RJson(@NotNull String key, JSONObject defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(@NotNull String value) {
        this.value = new JSONObject(value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    public boolean hasKey(String key) {
        return this.value.has(key);
    }

    public String getAsString(String key, String defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getString(key);
    }

    public int getAsInt(String key, int defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getInt(key);
    }

    public long getAsLong(String key, long defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getLong(key);
    }

    public double getAsDouble(String key, double defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getDouble(key);
    }

    public boolean getAsBoolean(String key, boolean defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getBoolean(key);
    }

    public <E extends Enum<E>> Enum<E> getAsEnum(String key, Class<E> enumClass, Enum<E> defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return E.valueOf(enumClass, this.value.getString(key));
    }

    public Number getAsNumber(String key, Number defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getNumber(key);
    }

    public void putString(String key, String value) {
        set(get().put(key, value));
    }

    public void putInt(String key, int value) {
        set(get().put(key, value));
    }

    public void putLong(String key, long value) {
        set(get().put(key, value));
    }

    public void putDouble(String key, double value) {
        set(get().put(key, value));
    }

    public void putBoolean(String key, boolean value) {
        set(get().put(key, value));
    }

    public void putEnum(String key, Enum<?> value) {
        set(get().put(key, value.name()));
    }

}
