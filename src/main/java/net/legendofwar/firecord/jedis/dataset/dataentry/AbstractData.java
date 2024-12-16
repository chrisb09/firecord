package net.legendofwar.firecord.jedis.dataset.dataentry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.DataEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.ClassNameLookup;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import redis.clients.jedis.Jedis;

public abstract class AbstractData<T> {

    // contains keys that were marked before constructor call to mark automatic creation by a DataGenerator
    // relevant for when it's deleted, so we can delete the tree
    // the permanent marking is part of the object, this set is only
    // temporary for creation purposes
    protected final static HashSet<Bytes> markedAsGenerated = new HashSet<>();

    public static HashMap<Bytes, AbstractData<?>> loaded = new HashMap<Bytes, AbstractData<?>>();
    protected final static Map<Bytes, List<Consumer<DataEvent<AbstractData<?>>>>> globalListeners = new HashMap<>();
    protected final static Map<Class<? extends AbstractData<?>>, List<Consumer<AbstractData<?>>>> objectCreationListener = new HashMap<>();
    private final static HashMap<Bytes, Consumer<DataEvent<AbstractData<?>>>> globalActiveListeners = new HashMap<>();
    private final static HashMap<Consumer<DataEvent<AbstractData<?>>>, Bytes> globalActiveListenersReverse = new HashMap<>();
    private static long globalActiveListenersAnonymousId = 0l;
    
        static {
    
            Firecord.subscribe(JedisCommunicationChannel.DEL_KEY, new MessageReceiver() {
    
                @Override
                public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                    AbstractData<?> ad = null;
                    synchronized (loaded) {
                        if (loaded.containsKey(message)) {
                            ad = loaded.get(message);
                        }
                    }
                    DataGenerator.delete(ad, false);
                }
    
            });
    
        }
    
        public static AbstractData<?> callConstructor(@NotNull Bytes key, @NotNull Class<?> c) {
    
            try {
                Constructor<?> constr = c.getDeclaredConstructor(Bytes.class);
                constr.setAccessible(true);
                return (AbstractData<?>) constr.newInstance(key);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    
        public static AbstractData<?> callConstructor(@NotNull Bytes key, @NotNull Class<?> c, Object defaultValue, Class<?> defaultValueClass) {
    
            try {
                Constructor<?> constr = c.getDeclaredConstructor(Bytes.class, defaultValueClass);
                constr.setAccessible(true);
                return (AbstractData<?>) constr.newInstance(key, defaultValue);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    
        /**
         * Creates the corresponding data structure for a key - if it exists that is.
         * If we already have loaded the corresponding datastructure return that
         * instead.
         * If you need a second instance for the same object for whatever terrible
         * reason do call the constructor.
         * 
         * @param key
         * @return
         */
        public static AbstractData<?> create(@NotNull Bytes key) {
    
            // since reflection is used the @NotNull annotation does not guarantee null
            // safety
            if (key == null) {
                return null;
            }
    
            // for null references
            if (key.length == 0){
                return null;
            }
    
            // we don't need to recreate
            synchronized (loaded) {
                if (loaded.containsKey(key)) {
                    return loaded.get(key);
                }
            }
            String type = null;
            try (Jedis j = ClassicJedisPool.getJedis()) {
                byte[] t = j.get(ByteFunctions.join(key, DataKeySuffix.TYPE));
                if (t != null) {
                    type = new String(t);
                }
            }
            if (type != null) {
                DataType dt = null;
                try {
                    dt = DataType.valueOf(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (dt != null) {
                    System.out.flush();
                    if (dt != DataType.OBJECT) {
                        if (dt.canBeLoaded()) {
                            AbstractData<?> object = callConstructor(key, dt.getC());
                            noteObjectCreation(object);
                            return object;
                        } else {
                            return new Invalid(key);
                        }
                    } else {
                        String className = null;
                        byte[] cN;
                        try (Jedis j = ClassicJedisPool.getJedis()) {
                            cN = j.get(ByteFunctions.join(key, DataKeySuffix.CLASS));
                        }
                        if (cN != null) {
                            className = ClassNameLookup.getClassName(new Bytes(cN));
                        }
                        Class<?> c = null;
                        if (className != null) {
                            try {
                                c = Class.forName(className);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                                Firecord.partialResource.registerUnavailableLoad(className, key);
                            }
                        }
                        if (c != null && AbstractObject.class.isAssignableFrom(c)
                                && !Modifier.isAbstract(c.getModifiers())) {
                            AbstractData<?> ad = callConstructor(key, c);
                            noteObjectCreation(ad);
                            Firecord.partialResource.registerLoad(c, ad);
                            return ad;
                        }
                    }
                }
            }
            return null;
    
        }

        public static void listenGlobal(Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
            Bytes b = new Bytes(globalActiveListenersAnonymousId++);
            synchronized (globalActiveListeners){
                while (globalActiveListeners.containsKey(b)){
                    globalActiveListenersAnonymousId++;
                    b = new Bytes(globalActiveListenersAnonymousId);
                }
            }
            listenGlobal(b, listener, channels);
        }
    
        public static void listenGlobal(Bytes listenerKey, Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
            boolean added = false;
            synchronized(globalListeners){
                for (JedisCommunicationChannel channel : channels) {
                    if (!globalListeners.containsKey(channel.getBytes())){
                        globalListeners.put(channel.getBytes(), new ArrayList<>());
                    }
                    globalListeners.get(channel.getBytes()).add(listener);
                    added = true;
                }
            }
            if (added){
                synchronized (globalActiveListeners){
                    globalActiveListeners.put(listenerKey, listener);
                    globalActiveListenersReverse.put(listener, listenerKey);
                }
            }
        }
    

    public static void stopListeningGlobal(Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
        boolean listenerStillActive = false;
        synchronized(globalListeners){
            for (JedisCommunicationChannel channel : channels) {
                if (globalListeners.containsKey(channel.getBytes())){
                    List<Consumer<DataEvent<AbstractData<?>>>> list = globalListeners.get(channel.getBytes());
                    while (list.contains(listener)){
                        list.remove(listener);
                    }
                    if (list.size() == 0){
                        globalListeners.remove(channel.getBytes());
                    }
                }
            }
            for (JedisCommunicationChannel channel : JedisCommunicationChannel.values()){
                if (globalListeners.containsKey(channel.getBytes())){
                    if (globalListeners.get(channel.getBytes()).contains(listener)){
                        listenerStillActive = true;
                        break;
                    }
                }
            }
        }
        synchronized(globalActiveListeners){
            if (!listenerStillActive) {
                if (globalActiveListenersReverse.containsKey(listener)){
                    Bytes key = globalActiveListenersReverse.remove(listener);
                    globalActiveListeners.remove(key);
                }
            }
        }
    }

    public static void stopListeningGlobal(Bytes listenerKey, JedisCommunicationChannel... channels) {
        synchronized(globalActiveListeners){
            if (globalActiveListeners.containsKey(listenerKey)){
                stopListeningGlobal(globalActiveListeners.get(listenerKey), channels);
            }
        }
    }

    public static void onObjectCreation(Consumer<AbstractData<?>> listener, Class<? extends AbstractData<?>> clazz){
        synchronized(AbstractData.objectCreationListener){
            if (!AbstractData.objectCreationListener.containsKey(clazz)){
                AbstractData.objectCreationListener.put(clazz, new ArrayList<Consumer<AbstractData<?>>>());
            }
            AbstractData.objectCreationListener.get(clazz).add(listener);
        }
    }

    private static void noteObjectCreation(AbstractData<?> object){
        synchronized(AbstractData.objectCreationListener){
            if (object != null && AbstractData.objectCreationListener.containsKey(object.getClass())){
                for (Consumer<AbstractData<?>> consumer : AbstractData.objectCreationListener.get(object.getClass())){
                    consumer.accept(object);
                }
            }
        }
    }

    protected final Bytes key;
    protected final JedisLock lock;
    protected int modifier; // 1: automatically generated

    protected final Map<Bytes, List<Consumer<DataEvent<AbstractData<?>>>>> listeners;

    private final HashMap<Bytes, Consumer<DataEvent<AbstractData<?>>>> activeListeners = new HashMap<>();
    private final HashMap<Consumer<DataEvent<AbstractData<?>>>, Bytes> activeListenersReverse = new HashMap<>();
    private long activeListenersAnonymousId = 0l;

    public ArrayList<AbstractData<?>> owners = new ArrayList<>();
    public long lastTimeOwnerBecameEmpty = 0l;

    /*
     * Notes related to owners (of this object)
     * 
     * Maybe RWrapper
     * 
     * RComposite
     *   RCollection
     *     RList [missing]
     * RMap [implemented-not_tested]
     * 
     * AbstractObject [missing]
     * 
     */

    protected AbstractData(@NotNull Bytes key) {
        this.key = key;
        this.listeners = new HashMap<>();
        if (key != null) {
            // make sure the object is NOT a temporary placeholde
            this.lock = new JedisLock(KeyGenerator.getLockKey(key));
            synchronized (loaded) {
                loaded.put(key, this);
            }
            try (Jedis j = ClassicJedisPool.getJedis()){
                byte[] bytes = j.get(key.getBytes().append(DataKeySuffix.MODIFIER).getData());
                if (bytes == null){
                    synchronized (markedAsGenerated){
                        modifier = markedAsGenerated.contains(key) ? 1 : 0;
                        markedAsGenerated.remove(key);
                    }
                    j.set(key.append(DataKeySuffix.MODIFIER).getData(), new Bytes(modifier).getData());
                } else {
                    modifier = (int) new Bytes(bytes).decodeNumber();
                }
                // load the owners, not sure if this is prudent as it causes other completely 
                // different reference trees to be loaded as well
                List<byte[]> owner_keys = j.lrange(key.getBytes().append(DataKeySuffix.OWNERS).getData(), 0, -1);
                for (byte[] owner_key : owner_keys){
                    Bytes b = new Bytes(owner_key);
                    AbstractData<?> ad = AbstractData.create(b);
                    if (ad != null){
                        owners.add(ad);
                    }
                    if (owners.size()==0){
                        lastTimeOwnerBecameEmpty = System.currentTimeMillis();
                    }
                }
            }
        } else {
            lock = null;
            modifier = 0;
        }
    }

    public JedisLock getJedisLock(){
        return this.lock;
    }

    public boolean tryLockMultiple(JedisLock... partners) {
        return this.lock.tryLockMultiple(partners);
    }

    public void unlockMultiple(JedisLock... partners) {
        this.lock.unlockMultiple(partners);
    }

    public JedisLock lock() {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        this.lock.lock();
        return this.lock;
    }

    public void close() {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        this.lock.unlock();
    }

    public void listen(Bytes listenerKey, Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
        boolean added = false;
        synchronized(this.listeners){
            for (JedisCommunicationChannel channel : channels) {
                if (!this.listeners.containsKey(channel.getBytes())){
                    this.listeners.put(channel.getBytes(), new ArrayList<>());
                }
                this.listeners.get(channel.getBytes()).add(listener);
                added = true;
            }
        }
        if (added){
            synchronized (this.activeListeners){
                this.activeListeners.put(listenerKey, listener);
                this.activeListenersReverse.put(listener, listenerKey);
            }
        }
    }

    public void listen(Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
        Bytes b = new Bytes(activeListenersAnonymousId++);
        synchronized (this.listeners){
            while (this.listeners.containsKey(b)){
                activeListenersAnonymousId++;
                b = new Bytes(activeListenersAnonymousId);
            }
        }
        listen(b, listener, channels);
    }

    public void stopListening(Consumer<DataEvent<AbstractData<?>>> listener, JedisCommunicationChannel... channels){
        boolean listenerStillActive = false;
        synchronized(this.listeners){
            for (JedisCommunicationChannel channel : channels) {
                if (this.listeners.containsKey(channel.getBytes())){
                    List<Consumer<DataEvent<AbstractData<?>>>> list = this.listeners.get(channel.getBytes());
                    while (list.contains(listener)){
                        list.remove(listener);
                    }
                    if (list.size() == 0){
                        this.listeners.remove(channel.getBytes());
                    }
                }
            }
            for (JedisCommunicationChannel channel : JedisCommunicationChannel.values()){
                if (this.listeners.containsKey(channel.getBytes())){
                    if (this.listeners.get(channel.getBytes()).contains(listener)){
                        listenerStillActive = true;
                        break;
                    }
                }
            }
        }
        synchronized(activeListeners){
            if (!listenerStillActive) {
                if (activeListenersReverse.containsKey(listener)){
                    Bytes key = activeListenersReverse.remove(listener);
                    activeListeners.remove(key);
                }
            }
        }
    }

    public void stopListening(Bytes listenerKey, JedisCommunicationChannel... channels) {
        synchronized(activeListeners){
            if (activeListeners.containsKey(listenerKey)){
                stopListening(activeListeners.get(listenerKey), channels);
            }
        }
    }

    public void stopListening(Consumer<DataEvent<AbstractData<?>>> listener){
        stopListening(listener, JedisCommunicationChannel.values());
    }

    public void stopListening(Bytes listenerKey) {
        stopListening(activeListeners.get(listenerKey), JedisCommunicationChannel.values());
    }

    private static List<Consumer<DataEvent<AbstractData<?>>>> getFittingGlobalListeners(JedisCommunicationChannel channel){
        return getFittingGlobalListeners(channel.getBytes());
    }

    private static List<Consumer<DataEvent<AbstractData<?>>>> getFittingGlobalListeners(Bytes channel){
        List<Consumer<DataEvent<AbstractData<?>>>> result = new ArrayList<>();
        synchronized (globalListeners){
            if (globalListeners.containsKey(channel)){
                result.addAll(globalListeners.get(channel));
            }
            if (globalListeners.containsKey(JedisCommunicationChannel.ANY.getBytes())){
                result.addAll(globalListeners.get(JedisCommunicationChannel.ANY.getBytes()));
            }
        }
        return result;
    }

    private List<Consumer<DataEvent<AbstractData<?>>>> getFittingListeners(JedisCommunicationChannel channel){
        return getFittingListeners(channel.getBytes());
    }

    private List<Consumer<DataEvent<AbstractData<?>>>> getFittingListeners(Bytes channel){
        List<Consumer<DataEvent<AbstractData<?>>>> result = new ArrayList<>();
        synchronized (this.listeners) {
            if (this.listeners.containsKey(channel)){
                result.addAll(this.listeners.get(channel));
            }
            if (this.listeners.containsKey(JedisCommunicationChannel.ANY.getBytes())){
                result.addAll(this.listeners.get(JedisCommunicationChannel.ANY.getBytes()));
            }
        }
        synchronized (globalListeners){
            if (globalListeners.containsKey(channel)){
                result.addAll(globalListeners.get(channel));
            }
            if (globalListeners.containsKey(JedisCommunicationChannel.ANY.getBytes())){
                result.addAll(globalListeners.get(JedisCommunicationChannel.ANY.getBytes()));
            }
        }
        return result;
    }

    protected boolean hasListeners() {
        return listeners.size() != 0;
    }

    public static void notifyListeners(AbstractData<?> affected, DataEvent<AbstractData<?>> event){
        if (affected != null){
            affected.notifyListeners(event);
        } else {
            List<Consumer<DataEvent<AbstractData<?>>>> listeners = getFittingGlobalListeners(event.getChannel());
            // notify listeners
            for (Consumer<DataEvent<AbstractData<?>>> consumer : listeners){
                try {
                    consumer.accept(event);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    protected void notifyListeners(DataEvent<AbstractData<?>> event){
        List<Consumer<DataEvent<AbstractData<?>>>> listeners = this.getFittingListeners(event.getChannel());
        if (hasListeners()){
            // notify listeners
            for (Consumer<DataEvent<AbstractData<?>>> consumer : listeners){
                try {
                    consumer.accept(event);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    protected void _setType(DataType dt) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(ByteFunctions.join(key, DataKeySuffix.TYPE), dt.name().getBytes());
        }
    }

    public final Bytes getKey() {
        return key;
    }

    // used for sorting, subclasses overwrite it
    public double getSortScore(){
        return this.key.hashCode();
    }

    protected void printTempErrorMsg(){
        System.out.println("Temporary Entries (key=null) cannot use their set, lock or get (basically any) functions, they only serve to initialize fields. Temporary entries are intended for initializing fields of objects inheriting from AbstractObject. They are automatically replaced with a fitting entry when using 'field = tempEntry' for a field of an AbstractObject. The key will also be null if this object has been overwritten by a new entry generated by a tempEntry.");
    }

}
