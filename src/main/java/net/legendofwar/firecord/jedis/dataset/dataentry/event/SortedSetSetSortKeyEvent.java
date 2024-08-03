package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public class SortedSetSetSortKeyEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.COLLECTION_CLEAR;

    final String oldSortKey;


    public SortedSetSetSortKeyEvent(Bytes instanceId, T affected, String oldSortKey) {
        super(instanceId, channel, affected);
        this.oldSortKey = oldSortKey;
    }

    public String getSortKey() {
        return this.oldSortKey;
    }

}
