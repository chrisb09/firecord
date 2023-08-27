package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class MapPutEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.MAP_PUT;

    final Bytes key;
    final T oldValue;
    final T newValue;

    public MapPutEvent(Bytes instanceId, T affected, Bytes key, T oldValue, T newValue) {
        super(instanceId, channel, affected);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Provides the key for the key->value pair that was affected
     * @return
     */
    public Bytes getKey() {
        return this.key;
    }

    public T getOldValue() {
        return this.oldValue;
    }

    public T getNewValue() {
        return this.newValue;
    }

}
