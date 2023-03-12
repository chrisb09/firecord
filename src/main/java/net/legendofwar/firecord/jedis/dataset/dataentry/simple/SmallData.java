package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.util.Arrays;
import java.util.Base64;

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public abstract class SmallData<T> extends SimpleData<T> {

    static{

        JedisCommunication.subscribe("update_key_small", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                String[] parts = message.split(":");
                String key = new String(Base64.getDecoder().decode(parts[0]));
                String value = String.join(":", Arrays.copyOfRange(parts, 1, parts.length, parts.getClass()));
                synchronized (loaded) {
                    if (loaded.containsKey(key)) {
                        ((SmallData<?>) loaded.get(key)).fromString(value);
                    }
                }
            }

        });

    }

    protected SmallData(String key, @NotNull T defaultValue, DataType dt) {
        super(key, defaultValue, dt);
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
                JedisCommunication.broadcast("del_key", this.key);
            }
        }
    }

}
