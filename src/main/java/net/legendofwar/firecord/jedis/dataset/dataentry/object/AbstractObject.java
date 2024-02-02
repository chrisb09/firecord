package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import net.legendofwar.firecord.jedis.JedisLock;
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
import net.legendofwar.firecord.jedis.dataset.datakeys.ClassNameLookup;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeyPrefix;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyLookupTable;
import redis.clients.jedis.Jedis;

public abstract class AbstractObject extends AbstractData<Object> {

    public static final HashMap<Bytes, AbstractObject> loaded = new HashMap<Bytes, AbstractObject>();

    private static final KeyLookupTable fieldKeyLookupTable = new KeyLookupTable(
            DataKeyPrefix.KEY_LOOKUP_TABLE.getBytes().append("fields".getBytes()), 4);

    public static Bytes getFieldKey(Bytes objectKey, String fieldName) {
        return objectKey.append(fieldKeyLookupTable.lookUpId(fieldName));
    }

    public static Bytes getStaticClassNameKey(String className) {
        return DataKeyPrefix.CLASS.getBytes().append(ClassNameLookup.getId(className));
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
            try (JedisLock lock = this.lock()) {
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
                    j.set(ByteFunctions.join(key, DataKeySuffix.CLASS), ClassNameLookup.getId(this.getClass().getName()).getData());
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
        ArrayList<Future<?>> asyncLoads = new ArrayList<>();
        Class<?> temp_c = c;
        Class<?> c_orig = c;
        String cN = null;
        if (object == null) { // only check for static call
            while (cN == null && temp_c != AbstractObject.class) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())){
                        if (field.getName().equals("STATIC_CLASSNAME_OVERWRITE")){
                            if (String.class.isAssignableFrom(field.getType())){
                                try {
                                    cN = (String) field.get(null);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                temp_c = temp_c.getSuperclass();
            }
        }
        if (cN == null){
            cN = c.getName();
        }
        final String className = cN;
        // repeat until we reach the AbstractObject superclass
        while (c != AbstractObject.class) {
            // iterate over all fields of the class
            final String _className = c.getName();
            for (final Field fi : c.getDeclaredFields()) {

                String fieldName = fi.getName();

                asyncLoads.add(ParallelWorkers.submit(_className, fieldName, new Runnable() {

                //populateField(fi, object, new Callable<Object>() {
                    
                    @Override
                    public void run() {
                    //public Object call() {
                        Field field = null;
                        try {
                            field = Class.forName(_className).getDeclaredField(fieldName);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        System.out.println("fi: "+fi.getClass()+" "+fi.getName());
                        System.out.println("fielld: "+field.getClass()+" "+field.getName());
                // only populate fields that are derived from AbstractData (have a key)
                if (AbstractData.class.isAssignableFrom(field.getType())) {
                    if (!AnnotationChecker.isFieldRestricted(field)){
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
                                        if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                            System.out.println("Before get "+field.getName()+" "+field.isAccessible());
                                            System.out.flush();
                                        }
                                        existingValue = field.get(object);
                                        if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                            System.out.println("After get");
                                            System.out.flush();
                                        }
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                    if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                        System.out.println("A");
                                        System.out.flush();
                                    }
                                    if (!references.containsKey(field.getName())) {

                                        if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                            System.out.println("B");
                                            System.out.flush();
                                        }
                                        // this field has no existing entry (in the object)
                                        if (existingValue == null) {


                                            if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                                System.out.println("C");
                                                System.out.flush();
                                            }
                                            final Bytes entryKey;
                                            if (Modifier.isStatic(field.getModifiers())) {
                                                if (object == null) { // init static fields only for the static call
                                                    entryKey = getFieldKey(getStaticClassNameKey(className),
                                                            field.getName());
                                                } else {
                                                    entryKey = null;
                                                }
                                            } else if (object != null) { // init object fields only when an instance is
                                                                        // passed
                                                entryKey = getFieldKey(object.key, field.getName());
                                            } else {
                                                entryKey = null;
                                            }
                                            if (entryKey != null) {


                                                if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                                    System.out.println("D");
                                                    System.out.flush();
                                                }

                                                //populateField(field, object, new Callable<Object>() {
                                                        
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
                                                        //return entry;

                                                // all DataEntries that don't have a corresponding entry in the underlaying
                                                // hash structure are added
                                                try (Jedis j = ClassicJedisPool.getJedis()) {
                                                    j.hset((object == null) ? getStaticClassNameKey(className).getData()
                                                            : object.key.getData(), new Bytes(field.getName()).getData(),
                                                            entryKey.getData());
                                                }
                                                references.put(field.getName(), entryKey);
                                            }
                                        }
                                    } else { // reference exists

                                        if (existingValue == null) {
                                            Bytes entryKey = references.get(field.getName());

                                            if (AnnotationChecker.isParallelLoadAllowed(c_orig) || AnnotationChecker.isParallelLoadAllowed(field)) {
                                                System.out.println("Create..."+entryKey);
                                                System.out.flush();
                                            }
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
                    }
                }));
            }
            c = c.getSuperclass();
        }

        /*if (Thread.currentThread().getId() == mainThread && setFields.size() > 0){
            waitOnCompletion(setFields);
        }*/
        waitOnCompletion(asyncLoads);

        if (AnnotationChecker.isParallelLoadAllowed(c_orig)) {
            System.out.println("Success");
        }

    }

    /*
     * Notes
     * 
     * 
     * It seems like reflecion doesnt "work" in parallel, at least if the field and such stems from the parent thread
     * but is accessed from the child...
     * 
     * Appraoches:
     * - Try if the field is created in the thread...
     * - only do the load async, not the setting
     * 
     * the latter approach has the issue that loading happens in the create(), which is supposed to return
     * the object we just loaded as it effectively just calls the constructor of the object.
     * 
     * essentially, the prudent approach would be to create a createAsnyc() function that returns a future, but
     * assuming reflection doesnt work outside the main thread at all it would mean that when this creates an object
     * then we'd be quite fkd ...
     * 
     * 
     * Or in other words: we need further testing to working changes
     * 
     */

    final static long mainThread = Thread.currentThread().getId();

    static BlockingQueue<SetField> setFields = new LinkedBlockingQueue<>();

    static void populateField(Field field, Object parent, Callable<Object> callable){
        setFields.add(new SetField(field, parent, ParallelWorkers.submit(parent.getClass().getName(), field.getName(), callable)));
    }

    record SetField(Field field, Object parent, Future<Object> future){

    };


    public static void waitOnCompletion(List<Future<?>> list) {
        if (list.size() > 1){
            System.out.println("Wait on " + list.size() + " Futures: "
                    + String.join(",", list.stream().map(x -> (x.isDone() + "")).toList()));
        }
        for (Future<?> f : list){
            try {
                f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (list.size() > 1){
            System.out.println("Wait done.");
        }
    }

    public static void waitOnCompletion(BlockingQueue<SetField> queue) {
        System.out.println("Wait on " + queue.size() + " Futures: "
                + String.join(",", queue.stream().map(x -> (x.future.isDone() + "")).toList()));
        while (!queue.isEmpty()){
            try {
                SetField sf = queue.take();
                Object entry = sf.future.get();
                if (entry != null) {
                    if (!(entry instanceof Invalid)) {
                        try {
                            sf.field.set(sf.parent, entry);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Wait done.");
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
                                Class<?> c = clazz;
                                while (!c.equals(Object.class) && !c.equals(AbstractData.class)){
                                    try {
                                        Field field = clazz.getDeclaredField(fieldName);
                                        field.setAccessible(true);
                                        field.set(obj, null);
                                        c = Object.class;
                                    } catch (NoSuchFieldException e) {
                                        if (c.equals(Object.class) || c.equals(AbstractData.class)){
                                            System.out.println("No such Field: " + fieldName + " [static=" + isStatic + "] in "
                                            + clazz.getName()+" or its subclasses");
                                            System.out.println("Known fields: "+String.join(",", Arrays.stream(c.getDeclaredFields()).map(field -> field.getName()).toList()));
                                        }
                                        c = c.getSuperclass();                                                
                                    }
                                }
                            } else {
                                AbstractData<?> referencedObject = AbstractData.create(referencedKey);
                                Class<?> c = clazz;
                                while (!c.equals(Object.class) && !c.equals(AbstractData.class)){
                                    try {
                                        Field field = c.getDeclaredField(fieldName);
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
                                        c = Object.class;
                                    } catch (NoSuchFieldException e) {
                                        if (c.equals(Object.class) || c.equals(AbstractData.class)){
                                            System.out.println("No such Field: " + fieldName + " [static=" + isStatic + "] in "
                                            + clazz.getName()+" or its subclasses");
                                            System.out.println("Known fields: "+String.join(",", Arrays.stream(c.getDeclaredFields()).map(field -> field.getName()).toList()));
                                        }
                                        c = c.getSuperclass();                                                
                                    }
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
