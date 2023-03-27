package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.Invalid;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import redis.clients.jedis.Jedis;

public abstract class AbstractObject extends AbstractData<Object> {

    public static final HashMap<String, AbstractObject> loaded = new HashMap<String, AbstractObject>();

    private static final HashMap<AbstractData<?>, AbstractObject> tempEntryParent = new HashMap<>();

    public static AbstractData<?> replaceTemp(AbstractData<?> entry){
        AbstractObject object = tempEntryParent.get(entry);
        // get field of containing object
        // get intended entry via object.key+::+fieldName
        // set entry to field
        // return intended entry
        return null;
    }

    protected RInteger RInteger(Integer defaultValue) {
        RInteger temp = new RInteger(null);
        temp.setIfEmpty(defaultValue);
        synchronized (tempEntryParent) {
            tempEntryParent.put(temp, this);
        }
        return temp;
    }

    protected AbstractObject(String key) {
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
                                    if (AbstractObject.class.isAssignableFrom(field.getType())){
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
