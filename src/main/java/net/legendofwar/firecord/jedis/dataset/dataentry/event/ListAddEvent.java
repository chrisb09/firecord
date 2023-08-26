package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListAddEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_ADD;

    final Object added;

    public ListAddEvent(Bytes instanceId, T affected, Object added) {
        super(instanceId, channel, affected);
        this.added = added;
    }

    public Object getAdded() {
        return this.added;
    }

}
