package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.Arrays;
import java.util.Base64;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;

public abstract class SmallData<T> extends SimpleData<T> {

    static {

        JedisCommunication.subscribe("update_key_small", new MessageReceiver() {

            @Override
            @SuppressWarnings("unchecked")
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                String value = String.join(":", Arrays.copyOfRange(parts, 1, parts.length, parts.getClass()));
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        SmallData<Object> sd = ((SmallData<Object>) loaded.get(key));
                        sd.fromString(value);
                        if (sd.listener != null) {
                            sd.listener.accept(sd);
                        }
                    }
                }
            }

        });

    }

    protected SmallData(String key, T defaultValue) {
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
            // we are using the default charset, if for some reason this is run on different
            // machines with different charsets this encoding would need to be changed.
            if (this.value != null) {
                String msg = Base64.getEncoder().encodeToString(this.key.getBytes()) + ":" + this.toString();
                JedisCommunication.broadcast("update_key_small", msg);
            } else {
                JedisCommunication.broadcast("del_key_value", this.key);
            }
        }
    }

}
