package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public abstract class DataEvent<T extends AbstractData<?>> {

    private final T affected;
    private final JedisCommunicationChannel channel;

    protected DataEvent(JedisCommunicationChannel channel, T affected){
        this.affected = affected;
        this.channel = channel;
    }

    public T getData(){
        return this.affected;
    }

    public JedisCommunicationChannel getChannel(){
        return channel;
    }
    
}
