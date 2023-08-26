package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SmallDataSetEvent<T extends AbstractData<?>> extends SimpleDataSetEvent<T> {

    public SmallDataSetEvent(JedisCommunicationChannel channel, T affected, Object oldValue) {
        super(channel, affected, oldValue);
    }

}
