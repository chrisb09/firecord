package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.List;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListSetEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_SET;

    final List<Object> oldValue;

    public ListSetEvent(Bytes instanceId, T affected, List<Object> oldValue) {
        super(instanceId, channel, affected);
        this.oldValue = oldValue;
    }

    public Object getOldValue() {
        return this.oldValue;
    }

}
