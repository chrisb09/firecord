package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SimpleDataDeleteEvent<T extends AbstractData<?>> extends DataEvent<T> {

    Object oldValue;

    public SimpleDataDeleteEvent(JedisCommunicationChannel channel, T affected, Object oldValue) {
        super(channel, affected);
        this.oldValue = oldValue;
    }

    public Object getOldValue() {
        return oldValue;
    }
}
