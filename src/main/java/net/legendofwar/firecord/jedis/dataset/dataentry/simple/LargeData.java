package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.concurrent.ThreadLocalRandom;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;

public abstract class LargeData<T> extends SimpleData<T> {

    static {

        JedisCommunication.subscribe("update_key_large", new MessageReceiver() {

            @Override
            @SuppressWarnings("unchecked")
            public void receive(String channel, String sender, boolean broadcast, String message) {
                SimpleData<Object> entry = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)) {
                        entry = (SimpleData<Object>) loaded.get(message);
                        entry.valid = false;
                        if (entry.listener != null) {
                            entry.listener.accept(entry);
                        }
                    }
                }
                if (entry != null) {
                    if (entry.getAggregateTime() != 0) {
                        synchronized (updateQueue) {
                            if (updateQueue.contains(entry)) {
                                updateQueue.remove(entry);
                            }
                            // we use random values to spread out the loading of different nodes
                            entry.timestamp_update = (int) ((System.currentTimeMillis()
                                    + ThreadLocalRandom.current().nextInt(entry.getAggregateTime()))
                                    % Integer.MAX_VALUE);
                            updateQueue.add(entry);
                        }
                    }
                }
            }

        });

    }

    LargeData(String key, T defaultValue) {
        super(key, defaultValue);
    }

    @Override
    int getAggregateTime() {
        // Update this key at most 10s after a change somewhere else happened
        // A higher value can save bandwith by skipping frequents changes
        // at the cost of a larger chance of a cache miss caused by outdated data
        return 10000;
    }

    @Override
    int getCacheTime() {
        // Unload after 60s without use
        // A higher value means we store the data for longer periods of time in
        // memory, reducing the chance of cache misses at the cost of a higher
        // memory usage
        return 60000;
    }

    @Override
    protected void _update(boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            if (this.value != null) {
                JedisCommunication.broadcast("update_key_large", this.key);
            } else {
                JedisCommunication.broadcast("del_key_value", this.key);
            }
        }
    }

}
