package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Collection;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListRemoveAllEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_REMOVE;

    final Collection<?> removed;

    public ListRemoveAllEvent(Bytes instanceId, T affected, Collection<?> removed) {
        super(instanceId, channel, affected);
        this.removed = removed;
    }

    public Collection<?> getRemoved() {
        return this.removed;
    }

}
