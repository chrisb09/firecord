package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListSetEvent<T extends AbstractData<?>, E extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_SET;

    final E oldValue;
    final int index;

    public ListSetEvent(Bytes instanceId, T affected, int index, E oldValue) {
        super(instanceId, channel, affected);
        this.oldValue = oldValue;
        this.index = index;
    }

    public Object getOldValue() {
        return this.oldValue;
    }

    public int getIndex() {
        return index;
    }

}
