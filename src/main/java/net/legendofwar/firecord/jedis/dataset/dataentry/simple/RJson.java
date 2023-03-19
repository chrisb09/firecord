package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class RJson extends LargeData<JSONObject> {

    public RJson(String key, JSONObject defaultValue) {
        super(key, defaultValue, DataType.JSON);
    }

    @Override
    public JSONObject get() {
        // return a copy of the JSONObject returned by super.get()
        return new JSONObject(super.get().toString());
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

    /*
    public Location getAsLocation(String key, Location defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return Location.deserialize(this.value.getJSONObject(key).toMap());
    }

    public ItemStack getAsItemStack(String key, ItemStack defaultValue) {
        if(!hasKey(key)) {
            return defaultValue;
        }
        String itemStackString = this.value.getString(key);
        return Minilib.getMethods().itemStackFromString(itemStackString);
    }*/

    public <E extends Enum<E>> Enum<E> getAsEnum(String key, Class<E> enumClass, Enum<E> defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return E.valueOf(enumClass, this.value.getString(key));
        //return this.value.getEnum(enumClass, key);
    }

    public Number getAsNumber(String key, Number defaultValue) {
        if (!hasKey(key)) {
            return defaultValue;
        }
        return this.value.getNumber(key);
    }

    public void putString(String key, String value) {
        this.value.put(key, value);
        _update();
    }

    public void putInt(String key, int value) {
        this.value.put(key, value);
        _update();
    }

    public void putLong(String key, long value) {
        this.value.put(key, value);
        _update();
    }

    public void putDouble(String key, double value) {
        this.value.put(key, value);
        _update();
    }

    public void putBoolean(String key, boolean value) {
        this.value.put(key, value);
        _update();
    }

    /*
    public void putLocation(String key, Location value) {
        this.value.put(key, value.serialize());
    }

    public void putItemStack(String key, ItemStack value) {
        this.value.put(key, Minilib.getMethods().itemStackToString(value));
    }*/

    public void putEnum(String key, Enum<?> value) {
        this.value.put(key, value.name());
        _update();
    }


}
