package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public abstract class LargeData<T> extends SimpleData<T> {

    static {

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
                    if (entry.getAggregateTime() != 0) {
                        synchronized (updateQueue) {
                            if (updateQueue.contains(entry)) {
                                updateQueue.remove(entry);
                            }
                            // we use random values to spread out the loading of different nodes
                            entry.timestamp_update = (int) ( (System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(entry.getAggregateTime())) % Integer.MAX_VALUE);
                            updateQueue.add(entry);
                        }
                    }
                }
            }

        });

    }

    LargeData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
    }

    @Override
    int getAggregateTime() {
        // Unload after 60s without use
        return 60000;
    }

    @Override
    int getCacheTime() {
        // Update this key at most 10s after a change somewhere else happened
        return 10000;
    }
    

    @Override
    protected void _update(boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            if (this.value != null) {
                JedisCommunication.broadcast("update_key_large", this.key);
            } else {
                JedisCommunication.broadcast("del_key", this.key);
            }
        }
    }

}
