package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.javatuples.Quartet;

import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.Invalid;
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
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeyPrefix;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyLookupTable;
import redis.clients.jedis.Jedis;

public abstract class AbstractObject extends AbstractData<Object> {

    public static final HashMap<Bytes, AbstractObject> loaded = new HashMap<Bytes, AbstractObject>();

    private static final KeyLookupTable fieldKeyLookupTable = new KeyLookupTable(
            DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append("fields".getBytes()), 4);

    private static final KeyLookupTable staticClassNameKeyLookupTable = new KeyLookupTable(
            DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append("static".getBytes()), 2);

    public static Bytes getFieldKey(Bytes objectKey, String fieldName) {
        return objectKey.append(fieldKeyLookupTable.lookUpId(fieldName));
    }

    public static Bytes getStaticClassNameKey(String className) {
        return DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append(new Bytes((byte) 0),
                staticClassNameKeyLookupTable.lookUpId(className));
    }

    /*
     * The following section is used for creating temporary entries
     */

    protected static RByte RByte(byte defaultValue) {
        return setTemp(RByte.class, defaultValue);
    }

    protected static RShort RShort(Short defaultValue) {
        return setTemp(RShort.class, defaultValue);
    }

    protected static RInteger RInteger(Integer defaultValue) {
        return setTemp(RInteger.class, defaultValue);
    }

    protected static RLong RLong(Long defaultValue) {
        return setTemp(RLong.class, defaultValue);
    }

    protected static RFloat RFloat(Float defaultValue) {
        return setTemp(RFloat.class, defaultValue);
    }

    protected static RDouble RDouble(Double defaultValue) {
        return setTemp(RDouble.class, defaultValue);
    }

    protected static RCharacter RChar(Character defaultValue) {
        return setTemp(RCharacter.class, defaultValue);
    }

    protected static RBoolean RBoolean(Boolean defaultValue) {
        return setTemp(RBoolean.class, defaultValue);
    }

    protected static RByteArray RByteArray(Bytes defaultValue) {
        return RByteArray(defaultValue.getData());
    }

    protected static RByteArray RByteArray(byte[] defaultValue) {
        return setTemp(RByteArray.class, defaultValue);
    }

    protected static RItemStack RItemStack(ItemStack defaultValue) {
        return setTemp(RItemStack.class, defaultValue);
    }

    protected static RJson RJson(JSONObject defaultValue) {
        return setTemp(RJson.class, defaultValue);
    }

    protected static RString RString(String defaultValue) {
        return setTemp(RString.class, defaultValue);
    }

    protected static RUUID RUUID(UUID defaultValue) {
        return setTemp(RUUID.class, defaultValue);
    }

    protected static RVector RVector(Vector defaultValue) {
        return setTemp(RVector.class, defaultValue);
    }

    protected static RWrapper RWrapper(Object defaultValue) {
        return setTemp(RWrapper.class, defaultValue, AbstractData.class);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Enum<T>> REnum<T> REnum(T defaultValue) {
        return (REnum<T>) setTemp(REnum.class, defaultValue);
    }

    private static <T extends AbstractData<?>> T setTemp(Class<T> c, Object defaultValue) {
        return setTemp(c, defaultValue, defaultValue.getClass());
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractData<?>> T setTemp(Class<T> c, Object defaultValue, Class<?> defaultValueClass) {
        T object = (T) AbstractData.callConstructor(null, c, defaultValue, defaultValueClass);
        return object;
    }

    boolean initialized = false;

    Map<String, Bytes> references;

    protected AbstractObject(@NotNull Bytes key) {
        super(key);
        if (key != null) {
            // make sure the object is NOT a temporary placeholde
            synchronized (loaded) {
                loaded.put(key, this);
            }
            try (AbstractData<?> ad = this.lock()) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    Map<byte[], byte[]> refs = j.hgetAll(ByteFunctions.join(key.getData()));
                    references = new HashMap<String, Bytes>();
                    for (Map.Entry<byte[], byte[]> en : refs.entrySet()) {
                        references.put(new Bytes(en.getKey()).asString(), new Bytes(en.getValue()));
                    }
                }
            }
            boolean hadNoEntries = references.isEmpty();
            loadObject(this.getClass(), this, references);
            if (hadNoEntries) {
                _setType(DataType.OBJECT);
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.set(ByteFunctions.join(key, DataKeySuffix.CLASS), this.getClass().getName().getBytes());
                }
            }
        }
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Loads all AbstractData entries that are NOT final
     * 
     * @param c
     * @param entries
     */
    @SuppressWarnings("null")
    static void loadObject(Class<?> c, AbstractObject object, Map<String, Bytes> references) {
        if (!AbstractObject.class.isAssignableFrom(c)) {
            System.out.println("The loadObject method is not defined for non-AbstractObjects.");
            return;
        }
        // repeat until we reach the AbstractObject superclass
        while (c != AbstractObject.class) {
            // iterate over all fields of the class
            for (Field field : c.getDeclaredFields()) {
                // only populate fields that are derived from AbstractData (have a key)
                if (AbstractData.class.isAssignableFrom(field.getType())) {
                    // only populate non-final fields
                    if (!Modifier.isFinal(field.getModifiers())) {
                        // only populate fields whose type is not abstract (RInteger, RList, etc. as
                        // opposed to AbstractData or SmallData)
                        // the user needs to init those fields themselves, preferably using tempEntries,
                        // which essentially means calling the constructor with a null key and assigning
                        // those to the field of the AbstractObject-subclass
                        if (!Modifier.isAbstract(field.getClass().getModifiers())) {
                            // only populate static fields for the static call(object=null) or non-static
                            // fields for the object call (object!=null)
                            if (Modifier.isStatic(field.getModifiers()) == (object == null)) {
                                field.setAccessible(true);
                                Object existingValue = null;
                                try {
                                    existingValue = field.get(object);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                if (!references.containsKey(field.getName())) {
                                    // this field has no existing entry (in the object)
                                    if (existingValue == null) {
                                        Bytes entryKey = null;
                                        if (Modifier.isStatic(field.getModifiers())) {
                                            if (object == null) { // init static fields only for the static call
                                                entryKey = getFieldKey(getStaticClassNameKey(c.getName()),
                                                        field.getName());
                                            }
                                        } else if (object != null) { // init object fields only when an instance is
                                                                     // passed
                                            entryKey = getFieldKey(object.key, field.getName());
                                        }
                                        if (entryKey != null) {
                                            AbstractData<?> entry = AbstractData.create(entryKey);
                                            if (entry == null) {
                                                DataType dt = DataType.getByC(field.getType());
                                                if (dt != null && dt.canBeLoaded()) {
                                                    markChild(object, entryKey);
                                                    entry = AbstractData.callConstructor(entryKey, field.getType());
                                                }
                                                if (dt == null) {
                                                    markChild(object, entryKey);
                                                    if (AbstractObject.class.isAssignableFrom(field.getType())) {
                                                        entry = AbstractData.callConstructor(entryKey, field.getType());
                                                    }
                                                }
                                            }
                                            if (entry != null) {
                                                if (!(entry instanceof Invalid)) {
                                                    try {
                                                        field.set(object, entry);
                                                    } catch (IllegalArgumentException e) {
                                                        e.printStackTrace();
                                                    } catch (IllegalAccessException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            // all DataEntries that don't have a corresponding entry in the underlaying
                                            // hash structure are added
                                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                                j.hset((object == null) ? getStaticClassNameKey(c.getName()).getData()
                                                        : object.key.getData(), new Bytes(field.getName()).getData(),
                                                        entryKey.getData());
                                            }
                                            references.put(field.getName(), entryKey);
                                        }
                                    }
                                } else { // reference exists
                                    if (existingValue == null) {
                                        Bytes entryKey = references.get(field.getName());
                                        AbstractData<?> entry = AbstractData.create(entryKey);
                                        if (entry == null && entryKey.length != 0) {
                                            DataType dt = DataType.getByC(field.getType());
                                            if (dt != null && dt.canBeLoaded()) {
                                                markChild(object, entryKey);
                                                entry = AbstractData.callConstructor(entryKey, field.getType());
                                            }
                                            if (dt == null) {
                                                markChild(object, entryKey);
                                                if (AbstractObject.class.isAssignableFrom(field.getType())) {
                                                    entry = AbstractData.callConstructor(entryKey, field.getType());
                                                }
                                            }
                                        }
                                        if (!(entry instanceof Invalid)) {
                                            if (entry == null || field.getType().isAssignableFrom(entry.getClass())) {
                                                try {
                                                    field.set(object, entry);
                                                } catch (IllegalArgumentException e) {
                                                    e.printStackTrace();
                                                } catch (IllegalAccessException e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                System.out.println("Error! Trying to assign field " + field.getName()
                                                        + "(" + field.getType().getName() + ") with an object of type "
                                                        + entry.getClass().getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private static void markChild(AbstractObject object, Bytes childKey){
        if (object != null){
            if (object.modifier % 2 == 1){
                synchronized (AbstractData.markedAsGenerated){
                    AbstractData.markedAsGenerated.add(childKey);
                }
            }
        }
    }

    static {

        LinkedBlockingQueue<Quartet<Bytes, String, Bytes, Boolean>> receiveQueue = new LinkedBlockingQueue<>();

        Thread thread = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        // blocks until an element is there
                        Quartet<Bytes, String, Bytes, Boolean> current = receiveQueue.take();
                        Bytes objectKey = current.getValue0();
                        String fieldName = current.getValue1();
                        Bytes referencedKey = current.getValue2();
                        Boolean isStatic = current.getValue3();

                        Class<?> clazz = null;
                        AbstractObject obj = null;

                        if (isStatic) {
                            try {
                                clazz = Class.forName(objectKey.asString());
                            } catch (ClassNotFoundException e) {
                                System.out.println("Could not find class " + objectKey);
                                e.printStackTrace();
                            }
                        } else {
                            if (AbstractObject.loaded.containsKey(objectKey)) {
                                obj = AbstractObject.loaded.get(objectKey);
                                clazz = obj.getClass();
                            }
                        }

                        // remove static entries
                        if (clazz != null) {
                            if (referencedKey.length == 0) {
                                // null reference
                                try {
                                    Field field = clazz.getDeclaredField(fieldName);
                                    field.setAccessible(true);
                                    field.set(obj, null);
                                } catch (NoSuchFieldException e) {
                                    System.out.println("No such Field: " + fieldName + " [static=" + isStatic + "] in "
                                            + clazz.getName());
                                }
                            } else {
                                AbstractData<?> referencedObject = AbstractData.create(referencedKey);
                                try {
                                    Field field = clazz.getDeclaredField(fieldName);
                                    field.setAccessible(true);

                                    // static entries use null in reflection as the object

                                    // if no change occurs we do not need to update the field
                                    if (referencedObject != field.get(obj)) {
                                        if (obj != null) {
                                            obj.references.put(field.getName(), referencedKey);
                                        }
                                        if (field.getType().isAssignableFrom(referencedObject.getClass())) {
                                            field.set(obj, referencedObject);
                                        }
                                    }
                                } catch (NoSuchFieldException e) {
                                    System.out.println("No such Field: " + fieldName + " [static=" + isStatic + "] in "
                                            + clazz.getName());
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        break;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        thread.start();

        JedisCommunication.subscribe(JedisCommunicationChannel.REFERENCE_UPDATE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Quartet<Bytes, Bytes, Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class,
                        Bytes.class, Bytes.class);
                Bytes objectKey = m.getValue0();
                Bytes fieldName = m.getValue1();
                Bytes value = m.getValue2();
                Boolean isStatic = m.getValue3().decodeNumber() != 0;
                receiveQueue.add(Quartet.with(objectKey, fieldName.asString(), value, isStatic));
            }

        });

        JedisCommunication.subscribe(JedisCommunicationChannel.OBJECT_OVERWRITE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // How do we deal with old references ?!?
                // Currently: just set the key to null, which will cause errors if the object
                // will be
                // used for get/set
                AbstractData<?> ad = null;
                synchronized (AbstractData.loaded) {
                    if (AbstractData.loaded.containsKey(message)) {
                        ad = AbstractData.loaded.get(message);
                    }
                }
                if (ad != null) {
                    removeFromLoaded(ad);
                }
            }

        });

    }

    private static void setKeyNull(AbstractData<?> ad) {
        try {
            Field field = AbstractData.class.getDeclaredField("key");
            field.setAccessible(true);
            field.set(ad, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static void removeFromLoaded(AbstractData<?> ad) {
        Class<?> c = ad.getClass();
        while (c != AbstractData.class) {
            removeFromLoaded(c, ad.getKey());
            c = c.getSuperclass();
        }
        removeFromLoaded(c, ad.getKey());
        setKeyNull(ad);
    }

    private static void removeFromLoaded(Class<?> c, Bytes key) {
        try {
            // Try to get the field from the class
            Field field = c.getDeclaredField("loaded");
            field.setAccessible(true);
            if (HashMap.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                HashMap<?, ?> map = ((HashMap<?, ?>) (field.get(null)));
                synchronized (map) {
                    if (map.containsKey(key)) {
                        map.remove(key);
                    }
                }
            }
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
