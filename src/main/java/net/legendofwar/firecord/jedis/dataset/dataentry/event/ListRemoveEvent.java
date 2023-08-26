package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListRemoveEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_REMOVE;

    final Object removed;

    public ListRemoveEvent(Bytes instanceId, T affected, Object removed) {
        super(instanceId, channel, affected);
        this.removed = removed;
    }

    public Object getRemoved() {
        return this.removed;
    }

}
