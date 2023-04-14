package net.legendofwar.firecord;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.communication.MessageReceiver;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.tool.FileIO;
import net.legendofwar.firecord.tool.NodeType;
import redis.clients.jedis.Jedis;

public class Firecord {

    private static Bytes id = null;
    private static NodeType nodeType = NodeType.STANDALONE;

    /**
     * Initialize the Firecord instance. ID's are not allowed to container ":".
     * 
     * @param id       Unique identifier of this node
     * @param nodeType Type of this node
     * @return boolean returns if the initialization is valid or not
     */
    public static boolean init(Bytes id, NodeType nodeType) {
        Firecord.id = id;
        Firecord.nodeType = nodeType;
        JedisCommunication.init(id);
        return id != null && id.length != 0;
    }

    /**
     * Initialize the Firecord instance.
     * 
     * @param nodeType Type of this node
     * @return boolean returns if the initialization is valid or not
     */
    public static boolean init(NodeType nodeType) {
        return init(new Bytes(loadId(new File("id"))), nodeType);
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
    public static Bytes getId() {
        return id;
    }

    /**
     * Get the id of this node.
     * 
     * @return String id of this node
     */
    public static String getIdName() {
        return id.asString();
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
     * @param channel  Channel to listen to. Channels are not allowed to contain ":"
     * @param receiver Receiver to handle the messages.
     */
    public static void subscribe(Bytes channel, MessageReceiver receiver) {
        JedisCommunication.subscribe(channel, receiver);
    }

    /**
     * Subscribe a receiver to a message channel. The receiver does not run in the
     * main thread so be mindful of this.
     * 
     * @param channel  Channel to listen to. Channels are not allowed to contain ":"
     * @param receiver Receiver to handle the messages.
     */
    public static void subscribe(JedisCommunicationChannel channel, MessageReceiver receiver) {
        JedisCommunication.subscribe(new Bytes(channel), receiver);
    }

    /**
     * Unsubscribe a certain channel.
     * 
     * @param channel Channel to unsubscribe from.
     */
    public static void unsubscribe(JedisCommunicationChannel channel) {
        JedisCommunication.unsubscribe(new Bytes(channel));
    }

    /**
     * Unsubscribe a certain channel.
     * 
     * @param channel Channel to unsubscribe from.
     */
    public static void unsubscribe(Bytes channel) {
        JedisCommunication.unsubscribe(channel);
    }

    /**
     * Send a message to a node, message to itself are allowed too.
     * 
     * @param receiver The node that should receive this message. Cannot contain
     *                 ":".
     * @param channel  The channel on which we send this message. Cannot contain
     *                 ":".
     * @param message  The message we send.
     */
    public static void publish(Bytes receiver, Bytes channel, Bytes message) {
        JedisCommunication.publish(receiver, channel, message);
    }

    /**
     * Send a message to a node, message to itself are allowed too.
     * 
     * @param receiver The node that should receive this message. Cannot contain
     *                 ":".
     * @param channel  The channel on which we send this message. Cannot contain
     *                 ":".
     * @param message  The message we send.
     */
    public static void publish(Bytes receiver, JedisCommunicationChannel channel, Bytes message) {
        JedisCommunication.publish(receiver, new Bytes(channel), message);
    }

    /**
     * Broadcast a message to all other nodes.
     * 
     * @param channel The channel on which we send this message. Cannot contain ":".
     * @param message The message we send.
     */
    public static void broadcast(Bytes channel, Bytes message) {
        JedisCommunication.broadcast(channel, message);
    }

    /**
     * Broadcast a message to all other nodes.
     * 
     * @param channel The channel on which we send this message. Cannot contain ":".
     * @param message The message we send.
     */
    public static void broadcast(JedisCommunicationChannel channel, Bytes message) {
        JedisCommunication.broadcast(new Bytes(channel), message);
    }

    /**
     * Get the IDs of all nodes currently active.
     * 
     * @return Set<String> Set of all IDs of nodes.
     */
    public static Set<Bytes> getNodes() {
        return JedisCommunication.getNodes();
    }

    public static Set<String> getNodeNames() {
        return new HashSet<String>(
                JedisCommunication.getNodes().stream().map(bytearray -> bytearray.asString()).toList());
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
