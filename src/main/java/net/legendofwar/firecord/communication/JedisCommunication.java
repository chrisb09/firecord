package net.legendofwar.firecord.communication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyGenerator;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyLookupTable;
import net.legendofwar.firecord.tool.Units;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class JedisCommunication extends BinaryJedisPubSub {

    private static byte[] id;
    private static Thread thread = null;
    private static Thread nodeThread = null;
    private static JedisCommunication handler = null;
    private static final HashMap<byte[], MessageReceiver> receivers = new HashMap<byte[], MessageReceiver>();
    private static final HashSet<byte[]> nodes = new HashSet<byte[]>();
    private static final JedisLock nodesLock = new JedisLock("nodes".getBytes());

    private final byte[][] channels;

    public static void init(byte[] id) {
        JedisCommunication.id = id;
        // smsg: server message, one for server specific and one for broadcast messages
        handler = new JedisCommunication(new byte[][] {KeyGenerator.join(JedisCommunicationChannels.SERVER_MESSAGE.getData(), id), JedisCommunicationChannels.SERVER_MESSAGE_BROADCAST.getData()});
        thread = new Thread(new Runnable() {

            public void run() {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    System.out.println("subscribe to: " + String.join(", ", Arrays.asList(handler.channels).stream().map(bytearray -> ByteFunctions.asHexadecimal(bytearray)).toList() ));
                    j.subscribe(handler, handler.channels);
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
        subscribe(JedisCommunicationChannels.TEST.getData(), new MessageReceiver() {

            @Override
            public void receive(byte[] channel, byte[] sender, boolean broadcast, byte[] message) {
                System.out.println("Receive test message from " + ByteFunctions.asHexadecimal(sender) + "(broadcast=" + broadcast + "): " + ByteFunctions.asHexadecimal(message));
            }

        });

        // log channel
        subscribe(JedisCommunicationChannels.LOG.getData(), new MessageReceiver() {

            @Override
            public void receive(byte[] channel, byte[] sender, boolean broadcast, byte[] message) {
                System.out.println("[" + ByteFunctions.asHexadecimal(sender) + "]: " + message);
            }

        });

        // ping channel
        subscribe("ping", new MessageReceiver() {

            @Override
            public void receive(byte[] channel, byte[] sender, boolean broadcast, byte[] message) {
                long delta = System.nanoTime() - ByteFunctions.decodeId(message); // we can reuse this function
                System.out.println(
                        "Received ping from " + ByteFunctions.asHexadecimal(sender) + "(broadcast=" + broadcast + "): " + Units.getTimeDelta(delta));
                publish(sender, "pong", message);
            }

        });

        // ping channel
        subscribe("pong", new MessageReceiver() {

            @Override
            public void receive(byte[] channel, byte[] sender, boolean broadcast, byte[] message) {
                long delta = System.nanoTime() - Long.parseLong(message);
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
                    Set<byte[]> found = j.smembers("nodes");
                    for (byte[] s : found) {
                        if (j.get("node:" + s) != null) {
                            nodes.add(s);
                        } else {
                            j.srem("nodes", s);
                        }
                    }
                    if (!nodes.contains(id)) {
                        nodes.add(id);
                        j.sadd("nodes", id);
                    }
                    j.setex("node:" + id, 10, "1"); // continuosly keep entry
                } finally {
                    nodesLock.unlock();
                }
            }
        }
    }

    public static Set<byte[]> getNodes() {
        return nodes;
    }

    public static void subscribe(byte[] channel, MessageReceiver receiver) {
        synchronized (receivers) {
            receivers.put(channel, receiver);
        }
    }

    public static void unsubscribe(byte[] channel) {
        synchronized (receivers) {
            if (receivers.containsKey(channel)) {
                receivers.remove(channel);
            }
        }
    }

    public static void publish(byte[] receiver, byte[] channel, byte[] message) {
        synchronized (nodes) {
            if (nodes.contains(receiver)) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.publish("smsg:" + receiver, channel + ":" + id + ":" + message);
                }
            }
        }
    }

    public static void broadcast(byte[] channel, byte[] message) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.publish("smsg:broadcast", channel + ":" + id + ":" + message);
        }
    }

    private JedisCommunication(byte[][] channels) {
        this.channels = channels;
    }

    private void receive(byte[] message, boolean broadcast) {
        byte[][] parts = message.split(":");
        byte[] channel = parts[0];
        byte[] sender = parts[1];
        if (sender.equals(id) && broadcast) {
            // don't receive messages from this node itself
            return;
        }
        synchronized (nodes) {
            if (!nodes.contains(sender)) {
                // in case a new node sends a message immediately we need to be able to answer
                nodes.add(sender);
            }
        }
        String rest = "";
        if (parts.length >= 3) {
            rest = parts[2];
        }
        for (int i = 3; i < parts.length; i++) {
            rest += ":" + parts[i];
        }
        MessageReceiver recv = null;
        synchronized (receivers) {
            if (receivers.containsKey(channel)) {
                recv = receivers.get(channel);
            } else {
                System.out.println("Received message in channel currently not handled by any listener.");
                System.out.println("Message channel: '" + channel + "'");
                System.out.println("Currently registred channels: " + String.join(", ", receivers.keySet()));
            }
        }
        if (recv != null) {
            recv.receive(channel, sender, broadcast, rest);
        }
    }

    @Override
    public void onMessage(byte[] redis_channel, byte[] message) {
        try {
            this.receive(message, redis_channel.equals("smsg:broadcast"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in message on redis_channel " + redis_channel);
            System.out.println("Message: " + message);
        }
    }

    @Override
    public void onPMessage(byte[] pattern, byte[] redis_channel, byte[] message) {
        this.receive(message, redis_channel.equals("smsg:broadcast"));
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
