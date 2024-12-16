package net.legendofwar.firecord.jedis.dataset.dataentry.event;

import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class ReferenceUpdateEvent<T extends AbstractData<?>> extends DataEvent<T> {

    Class<?> affectedClass;
    Object oldValue;
    Object newValue;
    String fieldName;
    boolean staticField;
    
    public ReferenceUpdateEvent(Bytes instanceId, JedisCommunicationChannel channel, T affected, Class<?> affectedClass, Object oldValue, Object newValue, String fieldName, boolean staticField){
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

    public Object getOldValue(){
        return oldValue;
    }

    public Object getNewValue(){
        return newValue;
    }

    public String getFieldName(){
        return fieldName;
    }

    public Object isStatic(){
        return staticField;
    }


}
