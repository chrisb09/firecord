package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class LargeDataSetEvent<T extends AbstractData<?>> extends SimpleDataSetEvent<T> {

    public LargeDataSetEvent(JedisCommunicationChannel channel, T affected, Object oldValue) {
        super(channel, affected, oldValue);
    }

}
