package net.legendofwar.firecord.jedis.dataset.dataentry.type;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import redis.clients.jedis.Jedis;

public abstract class SimpleData<T> implements Closeable {

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
    static HashMap<String, SimpleData<?>> loaded = new HashMap<String, SimpleData<?>>();

    // @formatter:off
	public final static long DEFAULT_CACHE_TIME = 0l;				// 0 means permanent
	public final static long DEFAUL_AGGREGATE_TIME = 10000000000l;  // 10s
    // @formatter:on

    static {

        JedisCommunication.subscribe("update_key_small", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                String value = String.join(":", Arrays.copyOfRange(parts, 1, parts.length, parts.getClass()));
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        ((SmallData<?>) loaded.get(key)).fromString(value);
                    }
                }
            }

        });

        JedisCommunication.subscribe("update_key_large", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                SimpleData<?> entry = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)) {
                        entry = loaded.get(message);
                        entry.valid = false;
                    }
                }
                if (entry != null) {
                    if (entry.aggregate_time != 0) {
                        synchronized (updateQueue) {
                            if (updateQueue.contains(entry)) {
                                updateQueue.remove(entry);
                            }
                            entry.timestamp_update = System.nanoTime() + entry.aggregate_time;
                            updateQueue.add(entry);
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
                            if (element.cache_time != 0) {
                                element.timestamp_unload = System.nanoTime() + element.cache_time;
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

                    long ts = System.nanoTime();

                    // Manage unloadQueue
                    synchronized (unloadQueue) {
                        element = unloadQueue.peek();
                        if (element != null && element.timestamp_unload < ts) {
                            element = unloadQueue.poll();
                            element.valid = false;
                            element.value = null;
                            element = unloadQueue.peek();
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
                            sd.get(); // forces loading if value isn't valid
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // don't care about interrupts
                    }
                }
            }

        });

        thread.start();

    }

    // @formatter:off
	long timestamp_unload = 0L;				        // timestamp in NS
	long timestamp_update = 0L;						// timestamp in NS
	long cache_time;					            // in NS
	long aggregate_time; 						    // in NS
	boolean valid = false;					        // determines if we hold a valid copy
    JedisLock lock;                                 // 
    String key;                                     //
    Object value;                                   //
	// @formatter:on

    SimpleData(String key, @NotNull T defaultValue, long cache_time, long aggregate_time) {
        this.key = key;
        this.cache_time = cache_time;
        this.aggregate_time = aggregate_time;
        this.lock = new JedisLock(key + ":lock");
        loaded.put(key, this);
        if (this.get() == null) {
            this.set(defaultValue);
        }
    }

    /**
     * 
     * @param key          the redis key this entry corresponds to
     * @param defaultValue the value we want to set if value does not exist
     * @param smallEntry   whether this entry's size is small - or not. determines
     *                     if it is unloaded and if the value is sent with update
     *                     notifications. only set to false for large values like
     *                     inventories.
     */
    SimpleData(String key, @NotNull T defaultValue, boolean smallEntry) {
        this(key, defaultValue, smallEntry ? 0 : 30 * 1000 * 1000 * 3000, smallEntry ? 0 : 10 * 1000 * 1000 * 3000);
    }

    SimpleData(String key, @NotNull T defaultValue) {
        this(key, defaultValue, true);
    }

    public SimpleData<T> lock() {
        this.lock.lock();
        return this;
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    protected void _update() {
        _update(true);
    }

    /**
     * 
     * Call whenever the value has been changed or loaded by this instance.
     * 
     * @param broadcast whether or not to announce changes to other nodes
     */
    protected void _update(boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            if (this instanceof SmallData) {
                // we are using the default charset, if for some reason this is run on different
                // machines with different charsets this encoding would need to be changed.
                String msg = Base64.getEncoder().encodeToString(this.key.getBytes()) + ":" + this.value.toString();
                JedisCommunication.broadcast("update_key_small", msg);
            } else {
                JedisCommunication.broadcast("update_key_large", this.key);
            }
        }
    }

    /**
     * Load a value from a string. Do NOT call _update()!
     * 
     * @param value
     */
    abstract protected void fromString(String value);

    public void set(T value) {
        this.value = value;
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.set(key, this.value.toString());
            this._update();
        }
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (!valid) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                this.fromString(j.get(key));
            }
            this._update(false);
        }
        return (T) this.value;
    }

}
