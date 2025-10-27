package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SimpleDataDeleteEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.SmallDataSetEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.AnnotationChecker;

public abstract class SmallData<T> extends SimpleData<T> {

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.UPDATE_SMALL_KEY, new MessageReceiver() {

            @Override
            @SuppressWarnings("unchecked")
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes value = m.getValue1();
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        SmallData<Object> sd = ((SmallData<Object>) loaded.get(key));
                        Object oldValue = sd.value;
                        sd.fromBytes(value);

                        sd.notifyListeners(
                                new SmallDataSetEvent<AbstractData<?>>(Firecord.getId(), sd, oldValue));
                    }
                }
            }

        });

    }

    protected SmallData(@NotNull Bytes key, T defaultValue) {
        super(key, defaultValue);
    }

    @Override
    int getAggregateTime() {
        return 0;
    }

    @Override
    int getCacheTime() {
        return 0;
    }

    @Override
    protected void _update(T oldValue, boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            boolean synchronize = AnnotationChecker.isSynchronizationEnabled(this.getClass()) && AnnotationChecker.isSynchronizationEnabled(this.value != null ? this.value.getClass() : null);
            if (this.value != null) {
                if (synchronize) {
                    JedisCommunication.broadcast(JedisCommunicationChannel.UPDATE_SMALL_KEY,
                            ByteMessage.write(this.key, this.toBytes()));
                }
                this.notifyListeners(
                        new SmallDataSetEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
            } else {
                if (synchronize) {
                    JedisCommunication.broadcast(JedisCommunicationChannel.DEL_KEY_VALUE, this.key);
                }
                this.notifyListeners(new SimpleDataDeleteEvent<AbstractData<?>>(Firecord.getId(), this, oldValue));
            }
        }
    }

}
