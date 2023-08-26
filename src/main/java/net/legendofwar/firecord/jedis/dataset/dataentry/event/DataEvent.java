package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public abstract class DataEvent<T extends AbstractData<?>> {

    private final Bytes instanceId;
    private final T affected;
    private final JedisCommunicationChannel channel;

    protected DataEvent(Bytes instanceId, JedisCommunicationChannel channel, T affected){
        this.instanceId = instanceId;
        this.affected = affected;
        this.channel = channel;
    }

    public Bytes getInstanceId(){
        return this.instanceId;
    }

    public boolean isRemote(){
        return !this.instanceId.equals(Firecord.getId());
    }

    public T getData(){
        return this.affected;
    }

    public JedisCommunicationChannel getChannel(){
        return channel;
    }
    
}
