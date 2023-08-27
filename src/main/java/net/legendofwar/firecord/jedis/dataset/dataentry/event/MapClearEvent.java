package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Map;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class MapClearEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.MAP_CLEAR;

    final Map<Bytes,T> oldValue;

    public MapClearEvent(Bytes instanceId, T affected, Map<Bytes,T> oldValue) {
        super(instanceId, channel, affected);
        this.oldValue = oldValue;
    }

    public Map<Bytes,T> getOldValue() {
        return this.oldValue;
    }

}
