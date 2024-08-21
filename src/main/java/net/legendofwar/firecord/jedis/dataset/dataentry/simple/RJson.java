package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import net.legendofwar.firecord.tool.SortHelper;

public final class RJson extends LargeData<JSONObject> {

    final static DataGenerator<RJson> GENERATOR = new DataGenerator<>(new Bytes("rjson"), RJson.class);

    final static RJson create() {
        return GENERATOR.create();
    }

    final static RJson create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RJson object) {
        DataGenerator.delete(object);
    }

    final static JSONObject DEFAULT_VALUE = new JSONObject();

    public RJson(@NotNull Bytes key) {
        this(key, null);
    }

    public RJson(@NotNull Bytes key, JSONObject defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes(this.value.toString());
    }

    @Override
    protected void fromBytes(@NotNull byte[] value) {
        this.value = new JSONObject(new String(value));
    }

    @Override
    public String toString() {
        get();
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


    public double getSortScore() {
        return SortHelper.getSortScore(this.toString());
    }

}
