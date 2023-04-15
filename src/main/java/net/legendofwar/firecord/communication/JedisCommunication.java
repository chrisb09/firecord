package net.legendofwar.firecord.communication;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.javatuples.Triplet;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeyPrefix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyLookupTable;
import net.legendofwar.firecord.tool.Units;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

public class JedisCommunication extends BinaryJedisPubSub {

    private static Bytes name;
    private static Thread thread = null;
    private static Thread nodeThread = null;
    private static JedisCommunication handler = null;
    private static final HashMap<Bytes, MessageReceiver> receivers = new HashMap<Bytes, MessageReceiver>();
    private static final HashSet<Bytes> nodes = new HashSet<Bytes>();
    private static final byte nodeIdSize = 1; // how many bytes are used for name encodings
    public static final KeyLookupTable nodeKeyLookUpTable = new KeyLookupTable(
            KeyGenerator.getLockKey(DataKeyPrefix.NODE.getBytes()), nodeIdSize);
    private static final Bytes nodes_key = DataKeyPrefix.NODE.getBytes();
    private static final JedisLock nodesLock = new JedisLock(nodes_key);

    private final Bytes[] channels;

    public static Bytes getBroadcastMessageChannel() {
        return JedisCommunicationChannel.SERVER_MESSAGE_BROADCAST.getBytes();
    }

    public static Bytes getServerMessageChannel(Bytes serverName) {
        return JedisCommunicationChannel.SERVER_MESSAGE.getBytes().append(nodeKeyLookUpTable.lookUpId(serverName));
    }

    public static void init(Bytes name) {
        JedisCommunication.name = name;
        // smsg: server message, one for server specific and one for broadcast messages
        System.out.println("ServerMessageChannel: " + getServerMessageChannel(name));
        System.out.println("BroadcastMessageChannel: " + getBroadcastMessageChannel());
        System.out.println("BroadcastMessageChannel.length: " + getBroadcastMessageChannel().length);
        handler = new JedisCommunication(new Bytes[] { getServerMessageChannel(name), getBroadcastMessageChannel() });
        thread = new Thread(new Runnable() {

            public void run() {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    System.out.println("subscribe to: " + String.join(", ", Arrays.stream(handler.channels)
                            .map(bytearray -> bytearray.toString()).toList()));
                    j.subscribe(handler, Arrays.stream(handler.channels)
                            .map(bytearray -> bytearray.getData()).toArray(byte[][]::new));
                }
            }
        });
        thread.start();
        updateNodes();
        nodeThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    updateNodes();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // does not matter
                    }
                }
            }
        });
        nodeThread.start();

        // test channel
        subscribe(JedisCommunicationChannel.TEST, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                System.out.println("Receive test message from " + sender + "(broadcast="
                        + broadcast + "): " + message);
            }

        });

        // log channel
        subscribe(JedisCommunicationChannel.LOG, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                System.out.println("[" + sender + "]: " + message);
            }

        });

        // ping channel
        subscribe(JedisCommunicationChannel.PING, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                long delta = System.nanoTime() - message.decodeNumber();
                System.out.println(
                        "Received ping from " + sender + "(broadcast=" + broadcast + "): "
                                + Units.getTimeDelta(delta));
                publish(sender, JedisCommunicationChannel.PONG, message);
            }

        });

        // ping channel
        subscribe(JedisCommunicationChannel.PONG, new MessageReceiver() {

            @Override
            public void receive(Bytes channel, Bytes sender, boolean broadcast, Bytes message) {
                long delta = System.nanoTime() - message.decodeNumber();
                System.out.println("Received pong from " + sender + "(broadcast=" + broadcast + "): rtt="
                        + Units.getTimeDelta(delta));
            }

        });
    }

    public static void disable() {
        if (handler != null) {
            handler.unsubscribe();
        }
    }

    private static void updateNodes() {
        synchronized (nodes) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                nodesLock.lock();
                try {
                    nodes.clear();
                    Set<byte[]> found = j.smembers(nodes_key.getData());
                    for (byte[] s : found) {
                        if (j.get(DataKeyPrefix.NODE.getBytes().append(s).getData()) != null) {
                            nodes.add(new Bytes(s));
                        } else {
                            j.srem(nodes_key.getData(), s);
                        }
                    }
                    if (!nodes.contains(name)) {
                        nodes.add(name);
                        j.sadd(nodes_key.getData(), name.getData());
                    }
                    // continuosly keep entry
                    j.setex(DataKeyPrefix.NODE.getBytes().append(name).getData(), 10, new Bytes(1).getData());
                } finally {
                    nodesLock.unlock();
                }
            }
        }
    }

    public static Set<Bytes> getNodes() {
        return nodes;
    }

    public static void subscribe(Bytes channel, MessageReceiver receiver) {
        synchronized (receivers) {
            receivers.put(channel, receiver);
        }
    }

    public static void subscribe(JedisCommunicationChannel channel, MessageReceiver receiver) {
        subscribe(channel.getBytes(), receiver);
    }

    public static void unsubscribe(Bytes channel) {
        synchronized (receivers) {
            if (receivers.containsKey(channel)) {
                receivers.remove(channel);
            }
        }
    }

    public static void unsubscribe(JedisCommunicationChannel channel) {
        unsubscribe(channel.getBytes());
    }

    public static void publish(Bytes receiver, Bytes channel, Bytes message) {
        if (channel.length != JedisCommunicationChannel.CHANNEL_BYTE_LENGTH) {
            throw new InvalidParameterException("The given channel has a length of " + channel.length
                    + " Bytes, while only " + JedisCommunicationChannel.CHANNEL_BYTE_LENGTH
                    + " Bytes are allowed. Channel: " + channel);
        }
        synchronized (nodes) {
            if (nodes.contains(receiver)) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.publish(getServerMessageChannel(receiver).getData(),
                            ByteMessage.write(channel, nodeKeyLookUpTable.lookUpId(name), message).getData());
                }
            }
        }
    }

    public static void publish(Bytes receiver, JedisCommunicationChannel channel, Bytes message) {
        publish(receiver, channel.getBytes(), message);
    }

    public static void broadcast(Bytes channel, Bytes message) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.publish(getBroadcastMessageChannel().getData(),
                    ByteMessage.write(channel, nodeKeyLookUpTable.lookUpId(name), message).getData());
        }
    }

    public static void broadcast(JedisCommunicationChannel channel, Bytes message) {
        broadcast(channel.getBytes(), message);
    }

    private JedisCommunication(Bytes[] channels) {
        this.channels = channels;
    }

    private void receive(Bytes message, boolean broadcast) {
        Triplet<Bytes, Bytes, Bytes> m = ByteMessage.readIn(message, Bytes.class, Bytes.class, Bytes.class);
        Bytes channel = m.getValue0();
        Bytes sender_id = m.getValue1();
        Bytes sender_name = nodeKeyLookUpTable.lookUpName(sender_id);
        Bytes content = m.getValue2();
        if (sender_name.equals(name) && broadcast) {
            // don't receive messages from this node itself
            return;
        }
        synchronized (nodes) {
            if (!nodes.contains(sender_name)) {
                // in case a new node sends a message immediately we need to be able to answer
                nodes.add(sender_name);
            }
        }
        MessageReceiver recv = null;
        synchronized (receivers) {
            if (receivers.containsKey(channel)) {
                recv = receivers.get(channel);
            } else {
                System.out.println("Received message in channel currently not handled by any listener.");
                System.out.println("Message channel: '" + channel + "'");
                System.out.println("Currently registred channels: " + String.join(", ",
                        receivers.keySet().stream().map(bytearray -> bytearray.toString()).toList()));
            }
        }
        if (recv != null) {
            recv.receive(channel, sender_name, broadcast, content);
        }
    }

    @Override
    public void onMessage(byte[] redis_channel, byte[] message) {
        try {
            this.receive(new Bytes(message),
                    Arrays.equals(redis_channel, JedisCommunicationChannel.SERVER_MESSAGE_BROADCAST.getData()));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in message on redis_channel " + new Bytes(redis_channel));
            System.out.println("Message: " + new Bytes(message));
        }
    }

    @Override
    public void onPMessage(byte[] pattern, byte[] redis_channel, byte[] message) {
        this.receive(new Bytes(message),
                Arrays.equals(redis_channel, JedisCommunicationChannel.SERVER_MESSAGE_BROADCAST.getData()));
    }

    @Override
    public void onSubscribe(byte[] channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(byte[] channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(byte[] pattern, int subscribedChannels) {

    }

}
