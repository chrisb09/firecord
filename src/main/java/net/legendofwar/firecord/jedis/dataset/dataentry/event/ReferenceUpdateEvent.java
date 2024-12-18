package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ReferenceUpdateEvent<T extends AbstractData<?>> extends DataEvent<T> {

    Class<?> affectedClass;
    T oldValue;
    T newValue;
    String fieldName;
    boolean staticField;
    
    public ReferenceUpdateEvent(Bytes instanceId, JedisCommunicationChannel channel, T affected, Class<?> affectedClass, T oldValue, T newValue, String fieldName, boolean staticField){
        super(instanceId, channel, affected);
        this.affectedClass = affectedClass;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.fieldName = fieldName;
        this.staticField = staticField;
    }

    public Class<?> getAffectedClass(){
        return affectedClass;
    }

    public T getOldValue(){
        return oldValue;
    }

    public T getNewValue(){
        return newValue;
    }

    public String getFieldName(){
        return fieldName;
    }

    public boolean isStatic(){
        return staticField;
    }


}
