package net.legendofwar.firecord.communication;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public interface MessageReceiver {

    /**
     * Simple method to receive a message from the redis server
     *
     * @param channel   The channel the message was sent to
     * @param sender    The sender of the message
     * @param broadcast Whether the message was sent to all servers or not
     * @param message   The message itself
     */
    void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message);

}
