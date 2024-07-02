package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SetRemoveEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.SET_REMOVE;

    final Object removed;

    public SetRemoveEvent(Bytes instanceId, T affected, Object removed) {
        super(instanceId, channel, affected);
        this.removed = removed;
    }

    public Object getRemoved() {
        return this.removed;
    }

}
