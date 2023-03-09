package net.legendofwar.firecord.communication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.JedisLock;
import net.legendofwar.firecord.tool.Units;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class JedisCommunication extends JedisPubSub {

    private static String id;
    private static Thread thread = null;
    private static Thread nodeThread = null;
    private static JedisCommunication handler = null;
    private static final HashMap<String, MessageReceiver> receivers = new HashMap<String, MessageReceiver>();
    private static final HashSet<String> nodes = new HashSet<String>();
    private static final JedisLock nodesLock = new JedisLock("nodes", 10000);

    private final String[] channels;

    public static void init(String id) {
        JedisCommunication.id = id;
        // smsg: server message, one for server specific and one for broadcast messages
        handler = new JedisCommunication(new String[] { "smsg:" + id, "smsg:broadcast" });
        thread = new Thread(new Runnable() {

            public void run() {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    System.out.println("subscribe to: "+String.join(", ", handler.channels));
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
        subscribe("test", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                System.out.println("Receive test message from "+sender+"(broadcast="+broadcast+"): "+message);
            }
            
        });

        // ping channel
        subscribe("ping", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                long delta = System.nanoTime() - Long.parseLong(message);
                System.out.println("Received ping from "+sender+"(broadcast="+broadcast+"): "+Units.getTimeDelta(delta));
                publish(sender, "pong", message);
            }
            
        });

        // ping channel
        subscribe("pong", new MessageReceiver() {

            @Override
            public void receive(String channel, String sender, boolean broadcast, String message) {
                long delta = System.nanoTime() - Long.parseLong(message);
                System.out.println("Received pong from "+sender+"(broadcast="+broadcast+"): rtt="+Units.getTimeDelta(delta));
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
                    Set<String> found = j.smembers("nodes");
                    for (String s : found) {
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

    public static Set<String> getNodes() {
        return nodes;
    }

    public static void subscribe(String channel, MessageReceiver receiver) {
        synchronized (receivers) {
            receivers.put(channel, receiver);
        }
    }

    public static void unsubscribe(String channel) {
        synchronized (receivers) {
            if (receivers.containsKey(channel)) {
                receivers.remove(channel);
            }
        }
    }

    public static void publish(String receiver, String channel, String message) {
        synchronized (nodes) {
            if (nodes.contains(receiver)) {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    j.publish("smsg:" + receiver, channel + ":" + id + ":" + message);
                }
            }
        }
    }

    public static void broadcast(String channel, String message) {
        try (Jedis j = ClassicJedisPool.getJedis()) {
            j.publish("smsg:broadcast", channel + ":" + id + ":" + message);
        }
    }

    private JedisCommunication(String[] channels) {
        this.channels = channels;
    }

    private void receive(String message, boolean broadcast) {
        String[] parts = message.split(":");
        String channel = parts[0];
        String sender = parts[1];
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
        String rest = parts[2];
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
                System.out.println("Currently registred channels: "+String.join(", ", receivers.keySet()));
            }
        }
        if (recv != null) {
            recv.receive(channel, sender, broadcast, rest);
        }
    }

    @Override
    public void onMessage(String redis_channel, String message) {
        this.receive(message, redis_channel.equals("smsg:broadcast"));
    }

    @Override
    public void onPMessage(String pattern, String redis_channel, String message) {
        this.receive(message, redis_channel.equals("smsg:broadcast"));
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }

}
