package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Collection;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class CollectionClearEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.COLLECTION_CLEAR;

    final Collection<?> oldValue;

    public CollectionClearEvent(Bytes instanceId, T affected, Collection<?> oldValue) {
        super(instanceId, channel, affected);
        this.oldValue = oldValue;
    }

    public Collection<?> getOldValue() {
        return this.oldValue;
    }

}
