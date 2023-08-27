package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import java.util.Map;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class MapPutAllEvent<T extends AbstractData<?>> extends DataEvent<T> {
    
    final static JedisCommunicationChannel channel = JedisCommunicationChannel.MAP_PUT_ALL;

    final Map<Bytes, ?> replaced;
    final Map<Bytes, ?> replacing;

    public MapPutAllEvent(Bytes instanceId, T affected, Map<Bytes, T> replaced, Map<Bytes, ?> replacing) {
        super(instanceId, channel, affected);
        this.replaced = replaced;
        this.replacing = replacing;
    }

    public Map<Bytes, ?> getReplaced() {
        return this.replaced;
    }

    public Map<Bytes, ?> getReplacing() {
        return this.replacing;
    }

}
