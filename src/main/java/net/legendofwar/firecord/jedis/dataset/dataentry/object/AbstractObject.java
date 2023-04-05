package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.Invalid;
import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByte;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RShort;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RLong;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RFloat;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RDouble;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RCharacter;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RByteArray;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RJson;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RUUID;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RVector;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RWrapper;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.SimpleData;
import redis.clients.jedis.Jedis;

public abstract class AbstractObject extends AbstractData<Object> {

    public static final HashMap<String, AbstractObject> loaded = new HashMap<String, AbstractObject>();

    // (temp-entry, parent)
    private static final HashMap<AbstractData<?>, AbstractObject> tempEntryParent = new HashMap<>();
    // (temp-entry, real-entry)
    private static final HashMap<AbstractData<?>, AbstractData<?>> alreadyReplaced = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <F extends AbstractData<?>> F replaceTemp(F entry) {
        synchronized (alreadyReplaced) {
            if (alreadyReplaced.containsKey(entry)) {
                return (F) alreadyReplaced.get(entry);
            }
        }
        AbstractObject object = null;
        synchronized (tempEntryParent) {
            object = tempEntryParent.get(entry);
        }
        F replacingEntry = null;
        // get field of containing object
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object existingEntry = null;
            try {
                existingEntry = field.get(object);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            if (existingEntry != null) {
                if (existingEntry.equals(entry)) {
                    // get intended entry via object.key+::+fieldName
                    String key = object.getKey() + "::" + field.getName();
                    replacingEntry = (F) AbstractData.create(key);
                    if (replacingEntry == null) {
                        if (entry instanceof SimpleInterface) {
                            Object dv = null;
                            if (entry instanceof SimpleData) {
                                dv = SimpleData.getValue(((SimpleData<?>) entry));
                            } else if (entry instanceof SimpleAbstractObject) {
                                dv = ((SimpleAbstractObject<?>) entry).getTempValue();
                            }
                            if (dv != null) {
                                replacingEntry = (F) callConstructor(key, entry.getClass(), dv);
                            } else {
                                replacingEntry = (F) callConstructor(key, entry.getClass());
                            }
                        } else {
                            // entry not suited ...
                            return entry;
                        }
                    }
                    try {
                        // set entry to field
                        field.set(object, replacingEntry);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (replacingEntry != null) {
            synchronized (alreadyReplaced) {
                alreadyReplaced.put(entry, replacingEntry);
            }
            synchronized (tempEntryParent) {
                tempEntryParent.remove(entry);
            }
        }
        // return intended entry
        return replacingEntry;
    }

    /*
     * The following section is used for creating temporary entries
     */

    protected RByte RByte(Byte defaultValue) {
        return setTemp(RByte.class, defaultValue);
    }

    protected RShort RShort(Short defaultValue) {
        return setTemp(RShort.class, defaultValue);
    }

    protected RInteger RInteger(Integer defaultValue) {
        return setTemp(RInteger.class, defaultValue);
    }

    protected RLong RLong(Long defaultValue) {
        return setTemp(RLong.class, defaultValue);
    }

    protected RFloat RFloat(Float defaultValue) {
        return setTemp(RFloat.class, defaultValue);
    }

    protected RDouble RDouble(Double defaultValue) {
        return setTemp(RDouble.class, defaultValue);
    }

    protected RCharacter RChar(Character defaultValue) {
        return setTemp(RCharacter.class, defaultValue);
    }

    protected RBoolean RBoolean(Boolean defaultValue) {
        return setTemp(RBoolean.class, defaultValue);
    }

    protected RByteArray RByteArray(byte[] defaultValue) {
        return setTemp(RByteArray.class, defaultValue);
    }

    protected RItemStack RItemStack(ItemStack defaultValue) {
        return setTemp(RItemStack.class, defaultValue);
    }

    protected RJson RJson(JSONObject defaultValue) {
        return setTemp(RJson.class, defaultValue);
    }

    protected RString RString(String defaultValue) {
        return setTemp(RString.class, defaultValue);
    }

    protected RUUID RUUID(UUID defaultValue) {
        return setTemp(RUUID.class, defaultValue);
    }

    protected RVector RVector(Vector defaultValue) {
        return setTemp(RVector.class, defaultValue);
    }

    protected RWrapper RWrapper(Object defaultValue) {
        return setTemp(RWrapper.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Enum<T>> REnum<T> REnum(T defaultValue) {
        return (REnum<T>) setTemp(REnum.class, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractData<?>> T setTemp(Class<T> c, Object defaultValue) {
        T object = (T) AbstractData.callConstructor(null, c, defaultValue);
        synchronized (tempEntryParent) {
            tempEntryParent.put(object, this);
        }
        return object;
    }

    protected AbstractObject(@NotNull String key) {
        super(key);
        if (key != null) {
            // make sure the object is NOT a temporary placeholde
            Set<String> existing = null;
            try (AbstractData<?> ad = this.lock()) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    existing = j.smembers(key);
                }
            }
            loadObject(this.getClass(), existing);
            if (existing.isEmpty()) {
                _setType(key, DataType.OBJECT);
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.set(key + ":class", this.getClass().getName());
                }
            }
            synchronized (loaded) {
                loaded.put(key, this);
            }
        }
    }

    /**
     * Loads all AbstractData entries that are NOT final
     * 
     * @param c
     * @param entries
     */
    private void loadObject(Class<?> c, Set<String> entries) {
        while (c != AbstractObject.class) {
            for (Field field : c.getDeclaredFields()) {
                if (AbstractData.class.isAssignableFrom(field.getType())) {
                    if (!Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        Object existingValue = null;
                        try {
                            existingValue = field.get(this);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if (existingValue == null) {
                            String entryKey = "";
                            if (Modifier.isStatic(field.getModifiers())) {
                                entryKey = c.getName() + "::" + field.getName();
                            } else {
                                entryKey = this.key + "::" + field.getName();
                            }
                            AbstractData<?> entry = AbstractData.create(entryKey);
                            if (entry == null) {
                                DataType dt = DataType.getByC(field.getType());
                                if (dt != null && dt.canBeLoaded()) {
                                    entry = AbstractData.callConstructor(entryKey, field.getType());
                                }
                                if (dt == null) {
                                    if (AbstractObject.class.isAssignableFrom(field.getType())) {
                                        entry = AbstractData.callConstructor(entryKey, field.getType());
                                    }
                                }
                            }
                            if (entry != null && !(entry instanceof Invalid)) {
                                try {
                                    field.set(this, entry);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (!entries.contains(field.getName())) {
                                // all DataEntries that don't have a corresponding entry in the underlaying set
                                // structure are added
                                try (Jedis j = ClassicJedisPool.getJedis()) {
                                    j.sadd(key, entryKey);
                                }
                                entries.add(entryKey);
                            }
                        }
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

}
