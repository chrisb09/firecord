package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Collection;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ListAddAllIndexEvent<T extends AbstractData<?>> extends DataEvent<T> {

    final static JedisCommunicationChannel channel = JedisCommunicationChannel.LIST_ADD_ALL_INDEX;

    final Collection<?> added;
    final int index;

    public ListAddAllIndexEvent(Bytes instanceId, T affected, Collection<?> added, int index) {
        super(instanceId, channel, affected);
        this.added = added;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public Collection<?> getAdded() {
        return this.added;
    }

}
