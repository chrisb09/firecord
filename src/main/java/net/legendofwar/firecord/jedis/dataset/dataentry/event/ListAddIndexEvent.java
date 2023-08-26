package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListAddIndexEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_ADD_INDEX;

    final Object added;
    final int index;

    public ListAddIndexEvent(Bytes instanceId, T affected, Object added, int index) {
        super(instanceId, channel, affected);
        this.added = added;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public Object getAdded() {
        return this.added;
    }

}
