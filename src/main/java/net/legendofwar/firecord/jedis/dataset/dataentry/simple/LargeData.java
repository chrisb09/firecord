package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.LargeDataSetEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SimpleDataDeleteEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AnnotationChecker;

public abstract class LargeData<T> extends SimpleData<T> {

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.UPDATE_LARGE_KEY, new MessageReceiver() {

            @Override
            @SuppressWarnings("unchecked")
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                LargeData<Object> entry = null;
                synchronized (loaded) {
                    if (loaded.containsKey(message)) {
                        entry = (LargeData<Object>) loaded.get(message);
                        entry.valid = false;
                        Object oldValue = entry.value;
                        entry.notifyListeners(
                                new LargeDataSetEvent<AbstractData<?>>(Firecord.getId(), entry, oldValue));

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

    LargeData(@NotNull Bytes key, T defaultValue) {
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
    protected void _update(T oldValue, boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            boolean synchronize = AnnotationChecker.isSynchronizationEnabled(this.getClass()) && AnnotationChecker.isSynchronizationEnabled(this.value != null ? this.value.getClass() : null);
            if (this.value != null) {
                if (synchronize) {
                    JedisCommunication.broadcast(JedisCommunicationChannel.UPDATE_LARGE_KEY, this.key);
                }
                this.notifyListeners(
                        new LargeDataSetEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
            } else {
                if (synchronize) {
                    JedisCommunication.broadcast(JedisCommunicationChannel.DEL_KEY_VALUE, this.key);
                }
                this.notifyListeners(new SimpleDataDeleteEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
            }
        }
    }

}
