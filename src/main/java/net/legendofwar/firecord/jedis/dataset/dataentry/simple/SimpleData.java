package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SimpleDataDeleteEvent;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import redis.clients.jedis.Jedis;

public abstract class SimpleData<T> extends AbstractData<T> implements SimpleInterface<T> {

    // Queue of entries which should be unloaded (in the future)
    static PriorityQueue<SimpleData<?>> unloadQueue = new PriorityQueue<SimpleData<?>>(new Comparator<SimpleData<?>>() {
        @Override
        public int compare(SimpleData<?> o1, SimpleData<?> o2) {
            return (int) (o1.timestamp_unload - o2.timestamp_unload);
        }
    });

    // Queue of entries which contain outdated values and need to be refreshed
    static PriorityQueue<SimpleData<?>> updateQueue = new PriorityQueue<SimpleData<?>>(new Comparator<SimpleData<?>>() {
        @Override
        public int compare(SimpleData<?> o1, SimpleData<?> o2) {
            return (int) (o1.timestamp_update - o2.timestamp_update);
        }
    });

    static Queue<SimpleData<?>> recentlyModified = new ConcurrentLinkedQueue<SimpleData<?>>();

    // Map of all (once) loaded SimpleData entries
    static HashMap<Bytes, SimpleData<?>> loaded = new HashMap<Bytes, SimpleData<?>>();

    private static LinkedBlockingQueue<Pair<SimpleData<?>, Object>> asyncSetQueue = new LinkedBlockingQueue<>();

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.DEL_KEY_VALUE, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                SimpleData<?> entry = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)) {
                        entry = loaded.get(message);
                        Object oldValue = entry.value;
                        entry.value = null;
                        entry.valid = true;
                        entry.notifyListeners(new SimpleDataDeleteEvent<AbstractData<?>>(Firecord.getId(), entry, oldValue));
                    }
                }
                if (entry != null) {
                    if (entry.getAggregateTime() != 0) {
                        synchronized (updateQueue) {
                            if (updateQueue.contains(entry)) {
                                updateQueue.remove(entry);
                            }
                        }
                    }
                    if (entry.getCacheTime() != 0) {
                        synchronized (unloadQueue) {
                            if (unloadQueue.contains(entry)) {
                                unloadQueue.remove(entry);
                            }
                        }
                    }
                }
            }

        });

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    // First, update the unload- and updateQueue with respect to the recently loaded
                    // or stored values
                    SimpleData<?> element = recentlyModified.poll();
                    while (element != null) {
                        synchronized (unloadQueue) {
                            if (element.getCacheTime() != 0) {
                                element.timestamp_unload = System.currentTimeMillis() + element.getCacheTime();
                                if (unloadQueue.contains(element)) {
                                    unloadQueue.remove(element);
                                }
                                unloadQueue.add(element);
                            }
                        }
                        synchronized (updateQueue) {
                            if (updateQueue.contains(element)) {
                                updateQueue.remove(element);
                            }
                        }
                        element = recentlyModified.poll();
                    }

                    long ts = System.currentTimeMillis();

                    // Manage unloadQueue
                    synchronized (unloadQueue) {
                        List<SimpleData<?>> toRem = new ArrayList<SimpleData<?>>();
                        element = unloadQueue.peek();
                        if (element != null && element.timestamp_unload < ts) {
                            element = unloadQueue.poll();
                            toRem.add(element);
                            element.valid = false;
                            element.value = null;
                            System.out.println("Unloaded " + element.key);
                            element = unloadQueue.peek();
                        }
                        synchronized (updateQueue) {
                            for (SimpleData<?> sd : toRem) {
                                if (updateQueue.contains(sd)) {
                                    updateQueue.remove(sd);
                                }
                            }
                        }
                    }

                    // Manage updateQueue
                    synchronized (updateQueue) {
                        element = updateQueue.peek();
                        List<SimpleData<?>> toGet = new ArrayList<SimpleData<?>>();
                        if (element != null && element.timestamp_update < ts) {
                            element = updateQueue.poll();
                            toGet.add(element);
                            element = updateQueue.peek();
                        }
                        for (SimpleData<?> sd : toGet) {
                            // forces loading if value isn't valid, modify=false means we don't reset
                            // the unload timer
                            sd.get(false);
                        }
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // don't care about interrupts
                    }
                }
            }

        });

        thread.start();

        Thread asyncSetThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Pair<SimpleData<?>, Object> data = asyncSetQueue.take();
                        SimpleData<?> simpleData = data.getValue0();
                        Object value = data.getValue1();
                        if (simpleData != null){
                            // Get a reference to the 'set' method
                            Class<?> clazz = simpleData.getClass();
                            while (clazz != null) {
                                for (Method method : clazz.getDeclaredMethods()){
                                    if (method.getName().equals("set") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(value.getClass())){
                                        try {
                                            method.setAccessible(true);
                                            method.invoke(simpleData, value);
                                            clazz = null;
                                        } catch (IllegalAccessException | InvocationTargetException e){
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                if (clazz != null) {
                                    if (SimpleData.class.equals(clazz)){
                                        clazz = null;
                                        System.out.println("Couldn't find a set method...");
                                    } else {
                                        clazz = clazz.getSuperclass();
                                    }
                                }
                            }
                    
                        }
                    } catch (InterruptedException e) {
                        break;
                    }

                }
            }

        });

        asyncSetThread.start();

    }

    // @formatter:off

    long timestamp_unload = 0;       // timestamp in ms
	int timestamp_update = 0;       // timestamp in ms
	boolean valid = false;          // determines if we hold a valid copy
    T value;                        // 
    T defaultValue;

	// @formatter:on

    @SuppressWarnings("unchecked")
    SimpleData(@NotNull Bytes key, T defaultValue) {
        super(key);
        DataType dt = DataType.getByC(this.getClass());
        T dv;
        if (defaultValue == null) {
            dv = (T) dt.getDefaultValue();
        } else {
            dv = defaultValue;
        }
        this.defaultValue = dv;
        if (key != null) {
            // make sure the object is NOT a temporary placeholder
            loaded.put(key, this);
            if (this._get(false) == null) {
                boolean alreadyExist = false;
                try (Jedis j = ClassicJedisPool.getJedis()){
                    byte[] t = j.get(ByteFunctions.join(key, DataKeySuffix.TYPE));
                    alreadyExist = t != null; // if null then this entry does not exist yet
                }
                if (!alreadyExist) {
                    this._setType(dt);
                    this.set(dv);
                }
            }
        } else {
            // entry is a temporary placeholder
            this.value = dv;
            valid = true;
        }
    }

    public final T getDefaultValue() {
        return this.defaultValue;
    }

    protected void _update(T oldValue) {
        _update(oldValue, true);
    }

    /**
     * 
     * Call whenever the value has been changed or loaded by this instance.
     * 
     * @param broadcast whether or not to announce changes to other nodes
     */
    protected abstract void _update(T oldValue, boolean broadcast);

    void _fromBytes(byte[] value) {
        if (value == null) {
            this.value = null;
        } else {
            fromBytes(value);
        }
    }

    /**
     * Load a value from a string. Do NOT call _update()!
     * 
     * @param value
     */
    abstract protected void fromBytes(@NotNull byte[] value);

    protected void fromBytes(@NotNull Bytes value) {
        this.fromBytes(value.getData());
    }

    /**
     * Gets byte[] representation of entry
     *
     */
    abstract protected Bytes toBytes();

    /**
     * Determine the time we keep the value of this entry in memory, in ms
     * 
     * @return
     */
    abstract int getCacheTime();

    /**
     * Determine the max time we wait until loading a recently changed value to deal
     * with rapidly changing entries efficiently, meaning not to load an entry 5
     * times just because it got changed 5 times in 3s.
     * 
     * @return
     */
    abstract int getAggregateTime();

    /**
     * Does not use a lock
     * 
     * @param value
     * @return true if change in database was successful
     */
    public boolean set(T value) {
        if (this.key == null) {
            printTempErrorMsg();
            return false;
        }
        T oldValue = this.value;
        this.value = value;
        boolean success = false;
        try (Jedis j = ClassicJedisPool.getJedis()) {
            if (value != null) {
                success = j.set(key.getData(), this.toBytes().getData()).equals("OK");
            } else {
                success = j.del(key.getData()) == 1;
            }
            this._update(oldValue);
        }
        return success;
    }

    public void setAsync(T value) {
        if (this.key == null) {
            printTempErrorMsg();
            return;
        }
        asyncSetQueue.add(Pair.with(this, value));
    }

    /**
     * checks whether the existing value is null OR equals to the default value
     * 
     * @param value
     * @return true if value was changed
     */
    public boolean setIfEmpty(T value) {
        if (this.key == null) {
            printTempErrorMsg();
            return false;
        }
        T v = get();
        T defaultValue = getDefaultValue();
        if (v == null || (defaultValue != null && defaultValue.equals(v))) {
            return set(value);
        }
        return false;
    }

    public boolean isEmpty(){
        if (this.key == null) {
            printTempErrorMsg();
            return false;
        }
        T v = get();
        T defaultValue = getDefaultValue();
        return v == null || (defaultValue != null && defaultValue.equals(v));
    }

    private T _get(boolean modify) {
        byte[] byteValue;
        try (Jedis j = ClassicJedisPool.getJedis()) {
            byteValue = j.get(key.getData());
        }
        T oldValue = this.value;
        if (byteValue != null) {
            this._fromBytes(byteValue);
        }
        if (modify) {
            this._update(oldValue, false);
        } else {
            this.valid = true;
        }
        return this.value;
    }

    private T get(boolean modify) {
        if (this.key == null) {
            printTempErrorMsg();
            return null;
        }
        if (!valid) {
            return _get(modify);
        }
        return this.value;
    }

    public T get() {
        return get(true);
    }

    public static <F> F getValue(SimpleData<F> object) {
        return object.value;
    }

}
