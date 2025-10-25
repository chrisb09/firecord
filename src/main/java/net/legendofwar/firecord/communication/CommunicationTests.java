package net.legendofwar.firecord.communication;

import java.lang.System.Logger.Level;

import org.javatuples.Triplet;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public class CommunicationTests {

    static {

        final Bytes testChannel = new Bytes("communicationTestChannel");
        JedisCommunication.subscribe(testChannel, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {

                Triplet<String, String, String> data = ByteMessage.readIn(message, String.class, String.class, String.class);

                String senderName = data.getValue0();
                String receiverName = data.getValue1();
                String content = data.getValue2();

                System.getLogger("firecord").log(Level.INFO, "Received message from " + senderName + " to " + receiverName + ": " + content);
                System.getLogger("firecord").log(Level.INFO, "  channel: " + channel);
                System.getLogger("firecord").log(Level.INFO, "  sender: " + sender);
                System.getLogger("firecord").log(Level.INFO, "  broadcast: " + broadcast);

            }

        }, true); // allow self messages

    }

    public static void init() {
        // static initializer
    }

    public static void sendMessage(String sender, String receiver, String content) {
        final Bytes testChannel = new Bytes("communicationTestChannel");
        Bytes message = ByteMessage.write(sender, receiver, content);
        JedisCommunication.broadcast(testChannel, message);
    }
    
}
