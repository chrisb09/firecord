package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Collection;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListAddAllEvent<T extends AbstractData<?>> extends DataEvent<T> {

    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_ADD_ALL;

    final Collection<?> added;

    public ListAddAllEvent(Bytes instanceId, T affected, Collection<?> added) {
        super(instanceId, channel, affected);
        this.added = added;
    }

    public Collection<?> getAdded() {
        return this.added;
    }

}
