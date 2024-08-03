package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SortedSetAddEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.SORTED_SET_ADD;

    final Object added;

    public SortedSetAddEvent(Bytes instanceId, T affected, Object added) {
        super(instanceId, channel, affected);
        this.added = added;
    }

    public Object getAdded() {
        return this.added;
    }

}
