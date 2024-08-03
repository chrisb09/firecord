package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Collection;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SortedSetRemoveAllEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.SORTED_SET_REMOVE_ALL;

    final Collection<?> removed;

    public SortedSetRemoveAllEvent(Bytes instanceId, T affected, Collection<?> removed) {
        super(instanceId, channel, affected);
        this.removed = removed;
    }

    public Collection<?> getRemoved() {
        return this.removed;
    }

}
