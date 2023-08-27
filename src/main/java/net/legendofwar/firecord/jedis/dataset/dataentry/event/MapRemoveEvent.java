package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class MapRemoveEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.MAP_REMOVE;

    final Bytes key;
    final T oldValue;

    public MapRemoveEvent(Bytes instanceId, T affected, Bytes key, T oldValue) {
        super(instanceId, channel, affected);
        this.key = key;
        this.oldValue = oldValue;
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

}
