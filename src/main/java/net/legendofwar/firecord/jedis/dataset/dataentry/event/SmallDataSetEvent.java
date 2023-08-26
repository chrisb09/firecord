package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SmallDataSetEvent<T extends AbstractData<?>> extends SimpleDataSetEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.UPDATE_SMALL_KEY;

    public SmallDataSetEvent(Bytes instanceId, T affected, Object oldValue) {
        super(instanceId, channel, affected, oldValue);
    }

}
