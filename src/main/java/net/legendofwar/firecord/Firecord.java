package net.legendofwar.firecord;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.tool.FileIO;
import net.legendofwar.firecord.tool.NodeType;
import redis.clients.jedis.Jedis;

public class Firecord {

    private static String id = null;
    private static NodeType nodeType = NodeType.STANDALONE;

    /**
     * Initialize the Firecord instance. ID's are not allowed to container ":".
     * 
     * @param id       Unique identifier of this node
     * @param nodeType Type of this node
     * @return boolean returns if the initialization is valid or not
     */
    public static boolean init(String id, NodeType nodeType) {
        Firecord.id = id;
        Firecord.nodeType = nodeType;
        JedisCommunication.init(id);
        return id != null && id.length() != 0;
    }

    /**
     * Initialize the Firecord instance. ID's are not allowed to container ":".
     * 
     * @param nodeType Type of this node
     * @return boolean returns if the initialization is valid or not
     */
    public static boolean init(NodeType nodeType) {
        return init(loadId(new File("id")), nodeType);
    }

    /**
     * This method should be called on disable to clean up open threads.
     */
    public static void disable() {
        JedisCommunication.disable();
    }

    /**
     * Get the id of this node.
     * 
     * @return String id of this node
     */
    public static String getId() {
        return id;
    }

    /**
     * Get the type of this node.
     * 
     * @return NodeType type of this node
     */
    public static NodeType getNodeType() {
        return nodeType;
    }

    /**
     * Get a Jedis instance. Remember to close() it after using in any case.
     * A try(Jedis j = Firecord.getJedis()){} should do the trick.
     * 
     * @return Jedis A Jedis instance.
     */
    public static Jedis getJedis() {
        return ClassicJedisPool.getJedis();
    }

    /**
     * Subscribe a receiver to a message channel. The receiver does not run in the
     * main thread so be mindful of this.
     * 
     * @param channel Channel to listen to. Channels are not allowed  to contain ":"
     * @param receiver Receiver to handle the messages.
     */
    public static void subscribe(String channel, MessageReceiver receiver) {
        JedisCommunication.subscribe(channel, receiver);
    }

    /**
     * Unsubscribe a certain channel.
     * 
     * @param channel Channel to unsubscribe from.
     */
    public static void unsubscribe(String channel) {
        JedisCommunication.unsubscribe(channel);
    }

    /**
     * Send a message to a node, message to itself are allowed too.
     * 
     * @param receiver The node that should receive this message. Cannot contain ":".
     * @param channel The channel on which we send this message. Cannot contain ":".
     * @param message The message we send.
     */
    public static void publish(String receiver, String channel, String message) {
        JedisCommunication.publish(receiver, channel, message);
    }

    /**
     * Broadcast a message to all other nodes.
     * 
     * @param channel The channel on which we send this message. Cannot contain ":".
     * @param message The message we send.
     */
    public static void broadcast(String channel, String message) {
        JedisCommunication.broadcast(channel, message);
    }

    /**
     * Get the IDs of all nodes currently active.
     * 
     * @return Set<String> Set of all IDs of nodes.
     */
    public static Set<String> getNodes() {
        return JedisCommunication.getNodes();
    }

    
    // hidden methods

    private static String loadId(File file) {
        String newId = null;
        try {
            newId = FileIO.readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newId;
    }

}
