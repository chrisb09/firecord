package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListRemoveIndexEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_REMOVE_INDEX;

    final Object removed;
    final int index;

    public ListRemoveIndexEvent(Bytes instanceId, T affected, Object removed, int index) {
        super(instanceId, channel, affected);
        this.removed = removed;
        this.index = index;
    }

    public Object getRemoved() {
        return this.removed;
    }

    public int getIndex(){
        return this.index;
    }

}
