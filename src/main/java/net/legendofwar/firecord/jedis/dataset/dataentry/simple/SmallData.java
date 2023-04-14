package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.dataset.Bytes;

public abstract class SmallData<T> extends SimpleData<T> {

    static {

        JedisCommunication.subscribe(JedisCommunicationChannel.UPDATE_SMALL_KEY, new MessageReceiver() {

            @Override
            @SuppressWarnings("unchecked")
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                // [2 byte, short, key-length in bytes: n][n Bytes: key][length - 2 - n byte:
                // value]
                Pair<Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class);
                Bytes key = m.getValue0();
                Bytes value = m.getValue1();
                /*
                 * ByteBuffer bytebuffer = ByteBuffer.wrap(message);
                 * bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
                 * short key_length = bytebuffer.getShort();
                 * byte[] key = new byte[key_length];
                 * for (int i = 0; i < key_length; i++) {
                 * key[i] = bytebuffer.get();
                 * }
                 * byte[] value = bytebuffer.array();
                 */
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        SmallData<Object> sd = ((SmallData<Object>) loaded.get(key));
                        sd.fromBytes(value);
                        if (sd.listener != null) {
                            sd.listener.accept(sd);
                        }
                    }
                }
            }

        });

    }

    protected SmallData(@NotNull Bytes key, T defaultValue) {
        super(key, defaultValue);
    }

    @Override
    int getAggregateTime() {
        return 0;
    }

    @Override
    int getCacheTime() {
        return 0;
    }

    @Override
    protected void _update(boolean broadcast) {
        this.valid = true;
        recentlyModified.add(this);
        if (broadcast) {
            if (this.value != null) {
                // [2 byte, short, key-length in bytes: n][n Bytes: key][length - 2 - n byte:
                // value]
                JedisCommunication.broadcast(JedisCommunicationChannel.UPDATE_SMALL_KEY,
                        ByteMessage.write(this.key, this.toBytes()));
                /*
                 * byte[] data = this.toBytes();
                 * ByteBuffer bytebuffer = ByteBuffer.allocate(2 + this.key.length +
                 * data.length);
                 * bytebuffer.putShort((short) data.length);
                 * bytebuffer.put(this.key);
                 * bytebuffer.put(data);
                 * bytebuffer.position(0);
                 * JedisCommunication.broadcast(JedisCommunicationChannel.UPDATE_SMALL_KEY,
                 * bytebuffer.array());
                 */
            } else {
                JedisCommunication.broadcast(JedisCommunicationChannel.DEL_KEY_VALUE, this.key);
            }
        }
    }

}
