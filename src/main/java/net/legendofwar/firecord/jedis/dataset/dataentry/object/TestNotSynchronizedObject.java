package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.util.function.Consumer;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.DataEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;

@ClassAnnotation(synchronize = false)
public class TestNotSynchronizedObject extends AbstractObject {

    public RInteger value;

    public TestNotSynchronizedObject(Bytes key) {
        super(key);

        this.listen(new Consumer<DataEvent<AbstractData<?>>>() {
            @Override
            public void accept(DataEvent<AbstractData<?>> event) {
                System.out.println("An event was fired on " + event.getChannel().name() + " from " + event.getInstanceId() + " with value " + ((RInteger) ((TestNotSynchronizedObject) (event.getData())).value).get() + " and isRemote=" + event.isRemote());
            }
        }, JedisCommunicationChannel.ANY);

        value.set((int) (Math.random() * 10));
    }
    
}
