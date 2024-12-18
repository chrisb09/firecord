package net.legendofwar.firecord.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RMap;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RSet;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.DataEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ReferenceUpdateEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.REnum;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.TestObject;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RLong;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeyPrefix;
import net.legendofwar.firecord.jedis.dataset.datakeys.KeyLookupTable;
import net.legendofwar.firecord.tool.NodeType;
import redis.clients.jedis.Jedis;

public class FirecordCommand {

    static DataGenerator<RLong> dg = new DataGenerator<>(new Bytes("longdata"), RLong.class);

    static KeyLookupTable testKeyLookupTable;
    static RBoolean testbool = null;
    static RInteger test = null;
    static RInteger test1 = null;
    static RInteger test2 = null;
    static RInteger test3 = null;
    static Object testis = null;
    static TestObject testob = null;
    static REnum<DataKeyPrefix> testenum = null;
    static RList<RInteger> testlist1 = null;
    static RList<RInteger> testlist2 = null;
    static RList<AbstractData<?>> testlist3 = null;
    static RSet<RInteger> testset1 = null;
    static RSet<RInteger> testset2 = null;
    static RSet<AbstractData<?>> testset3 = null;
    
    static RMap<RInteger> testmap1 = null;
    static RMap<RInteger> testmap2 = null;
    static RMap<AbstractData<?>> testmap3 = null;

    static boolean testlistener_active = false;
    static Consumer<DataEvent<AbstractData<?>>> testlistener = new Consumer<>() {

        @Override
        public void accept(DataEvent<AbstractData<?>> event) {
            System.out.println("[Testlistener][" + event.getChannel().name() + "][" + event.getInstanceId() + "]: "
                    + event.getData().getKey());
        }

    };

    static boolean testreferencelistener_active = false;
    static Consumer<DataEvent<AbstractData<?>>> referenceUpdateListener = new Consumer<DataEvent<AbstractData<?>>>() {

        @Override
        public void accept(DataEvent<AbstractData<?>> event) {
            if (event instanceof ReferenceUpdateEvent referenceUpdateEvent){
                System.out.println("[ReferenceUpdateListener]["+(referenceUpdateEvent.isStatic() ? referenceUpdateEvent.getClass().getSimpleName() : referenceUpdateEvent.getData().getClass().getSimpleName()+":" + referenceUpdateEvent.getData().getKey().toString()) + "][" + referenceUpdateEvent.getFieldName() + "] " + (referenceUpdateEvent.getOldValue() == null ? "null" : referenceUpdateEvent.getOldValue().getKey()) + " --> " + (referenceUpdateEvent.getNewValue() == null ? "null" : referenceUpdateEvent.getNewValue().getKey()) ); 
            }
        }
        
    };

    public static boolean onCommand(Sender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b" + label + " id          §e show id of this node");
            sender.sendMessage("§b" + label + " ids/list    §e show ids of all nodes");
            sender.sendMessage("§b" + label + " test        §e broadcast test message");
            sender.sendMessage("§b" + label + " testid      §e test the KeyLookupTable");
            sender.sendMessage("§b" + label + " testint     §e small data sync example");
            sender.sendMessage("§b" + label + " loadis      §e large data sync example");
            sender.sendMessage("§b" + label + " storeis     §e large data sync example");
            sender.sendMessage("§b" + label + " bytes <key> §e redis access using hex encoding");
            sender.sendMessage("§b" + label + " testfield   §e test a field change of testob");
            sender.sendMessage("§b" + label + " teststatic  §e testfield for static vars");
            sender.sendMessage("§b" + label + " testchar    §e runs RChar related test");
            sender.sendMessage("§b" + label + " testbool    §e runs RBoolean test");
            sender.sendMessage("§b" + label + " testnull    §e test null toggle");
            sender.sendMessage("§b" + label + " testlisten  §e toggle test listener");
            sender.sendMessage("§b" + label + " testreferencelisten  §e toggle test reference listener");
            sender.sendMessage("§b" + label + " testow      §e test overwrite");
            sender.sendMessage("§b" + label + " testasync   §e test setAsync");
            sender.sendMessage("§b" + label + " tested      §e test encode-decode");
            sender.sendMessage("§b" + label + " testlist    §e list test command");
            sender.sendMessage("§b" + label + " testset     §e set test command");
            sender.sendMessage("§b" + label + " testmap     §e map test command");
            sender.sendMessage("§b" + label + " testanon    §e anonymous type test command");
            sender.sendMessage("§b" + label + " testmessage §e write&read to a test-byte[]");
            sender.sendMessage("§b" + label + " testobject  §e test object command");
            sender.sendMessage("§b" + label + " testenum    §e construct an enum");
            sender.sendMessage("§b" + label + " ping <node> §e broadcast test message");
            sender.sendMessage("§b" + label + " redis <key> §e get redis entry at key");
            sender.sendMessage("§b" + label + " help        §e show this help page");
        } else if (args[0].equalsIgnoreCase("id")) {
            sender.sendMessage("§bid: §e" + Firecord.getId().asString());
        } else if (args[0].equalsIgnoreCase("ids") || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§bids: §e" + String.join(", ", Firecord.getNodeNames()));
        } else if (args[0].equalsIgnoreCase("test")) {
            sender.sendMessage("§7send broadcast message to all other nodes that causes a entry in the log.");
            Firecord.broadcast(JedisCommunicationChannel.TEST, new Bytes("Hello World"));
        } else if (args[0].equalsIgnoreCase("serialize")) {
            if (test == null) {
                test = new RInteger(new Bytes("testint"));
                test.setIfEmpty(0);
            }
        } else if (args[0].equalsIgnoreCase("testmap")) {
            if (testmap1 == null) {
                testmap1 = new RMap<RInteger>(new Bytes("testmap1"));
            }
            if (testmap2 == null) {
                testmap2 = new RMap<RInteger>(new Bytes("testmap2"));
            }
            if (testmap3 == null) {
                testmap3 = new RMap<AbstractData<?>>(new Bytes("testmap3"));
            }
            sender.sendMessage("§btestmap1: §e"+testmap1.getKey().toRedisString());
            sender.sendMessage("§btestmap2: §e"+testmap2.getKey().toRedisString());
            sender.sendMessage("§btestmap3: §e"+testmap3.getKey().toRedisString());
            if (test1 == null) {
                test1 = (RInteger) AbstractData.create(new Bytes("testint1"));
                if (test1 == null) {
                    test1 = new RInteger(new Bytes("testint1"));
                    test1.set(1);
                }
            }
            if (test2 == null) {
                test2 = (RInteger) AbstractData.create(new Bytes("testint2"));
                if (test2 == null) {
                    test2 = new RInteger(new Bytes("testint2"));
                    test2.setIfEmpty(2);
                }
            }
            if (test3 == null) {
                test3 = (RInteger) AbstractData.create(new Bytes("testint3"));
                if (test3 == null) {
                    test3 = new RInteger(new Bytes("testint3"));
                    test3.setIfEmpty(3);
                }
            }
            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("§btestmap1: §e" + String.join(",", Arrays.toString(testmap1.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));
            sender.sendMessage("§btestmap2: §e" + String.join(",", Arrays.toString(testmap2.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));
            sender.sendMessage("§btestmap3: §e" + String.join(",", Arrays.toString(testmap3.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));
            sender.sendMessage("§bClear testmaps");
            testmap1.clear();
            testmap2.clear();
            testmap3.clear();
            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            RInteger a = (RInteger) AbstractData.create(new Bytes("testint" + (1 + ((int) (Math.random() * 3)))));
            RInteger b = (RInteger) AbstractData.create(new Bytes("testint" + (1 + ((int) (Math.random() * 3)))));
            RInteger c = (RInteger) AbstractData.create(new Bytes("testint" + (1 + ((int) (Math.random() * 3)))));
            sender.sendMessage("§a#1 put a:" + a + ", b:" + b + ", c:" + c);
            testmap1.put(new Bytes("a"), a);
            testmap1.put(new Bytes("b"), b);
            testmap1.put(new Bytes("c"), c);
            sender.sendMessage("§btestmap1: §e" + testmap1.size());
            sender.sendMessage("§btestmap1: §e" + String.join(",", Arrays.toString(testmap1.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));

            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("§a#2 put a:3, d:3");
            testmap2.put(new Bytes("a"), test3);
            testmap2.put(new Bytes("d"), test3);

            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            
            sender.sendMessage("§a#2 putAll #1");
            testmap2.putAll(testmap1);
            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("§btestmap2: §e" + testmap2.size());
            sender.sendMessage("§btestmap2: §e" + String.join(",", Arrays.toString(testmap2.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));

            sender.sendMessage("§a#2 remove b");
            testmap2.remove(new Bytes("b"));
            sender.sendMessage("§btestmap2: §e" + testmap2.size());
            sender.sendMessage("§btestmap2: §e" + String.join(",", Arrays.toString(testmap2.entrySet().stream()
                    .map(entry -> entry.getKey().asString() + ": " + entry.getValue().toString()).toArray())));
            sender.sendMessage("test1-owners: "+String.join(",", test1.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test2-owners: "+String.join(",", test2.owners.stream().map(owner -> owner.getKey().asString()).toList()));
            sender.sendMessage("test3-owners: "+String.join(",", test3.owners.stream().map(owner -> owner.getKey().asString()).toList()));
        } else if (args[0].equalsIgnoreCase("testlist")) {
            if (testlist1 == null) {
                testlist1 = new RList<RInteger>(new Bytes("testlist1"));
            }
            if (testlist2 == null) {
                testlist2 = new RList<RInteger>(new Bytes("testlist2"));
            }
            if (testlist3 == null) {
                testlist3 = new RList<AbstractData<?>>(new Bytes("testlist3"));
            }
            if (test1 == null) {
                test1 = (RInteger) AbstractData.create(new Bytes("testint1"));
                if (test1 == null) {
                    test1 = new RInteger(new Bytes("testint1"));
                    test1.set(1);
                }
            }
            if (test2 == null) {
                test2 = (RInteger) AbstractData.create(new Bytes("testint2"));
                if (test2 == null) {
                    test2 = new RInteger(new Bytes("testint2"));
                    test2.setIfEmpty(2);
                }
            }
            if (test3 == null) {
                test3 = (RInteger) AbstractData.create(new Bytes("testint3"));
                if (test3 == null) {
                    test3 = new RInteger(new Bytes("testint3"));
                    test3.setIfEmpty(3);
                }
            }
            Bytes logKey = testlist2.getKey();
            sender.sendMessage("§btestlist1: §e" + String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§btestlist2: §e" + String.join(",", Arrays.toString(testlist2.toArray())));
            sender.sendMessage("§btestlist3: §e" + String.join(",", Arrays.toString(testlist3.toArray())));
            sender.sendMessage("§a#1,#2,#3 clear");
            testlist1.clear();
            testlist2.clear();
            testlist3.clear();
            sender.sendMessage("§btestlist1: §e" + testlist1.size());
            sender.sendMessage("§a#1 add Elements 1,2,3");
            testlist1.add(test1);
            testlist1.add(test2);
            testlist1.add(test3);
            sender.sendMessage("§btestlist1: §e" + testlist1.size());
            sender.sendMessage("§btestlist1: §e" + String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§a#2 add Elements of list #1");
            testlist2.addAll(testlist1);
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After first add: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testlist2.toArray()))));
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_LOG, logKey);
            sender.sendMessage("§a#2 add Elements of list #1 at position 1");
            testlist2.addAll(1, testlist1);
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After second add: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testlist2.toArray()))));
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_LOG, logKey);
            sender.sendMessage("§a#2 add Elements of list #1 at position 3");
            testlist2.addAll(3, testlist1);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e" + String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After adding: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testlist2.toArray()))));
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_LOG, logKey);
            sender.sendMessage("§a#3 add Element 3");
            testlist3.add(test3);
            sender.sendMessage("§btestlist3: §e" + testlist3.size());
            sender.sendMessage("§btestlist3: §e" + String.join(",", Arrays.toString(testlist3.toArray())));
            sender.sendMessage("§a#1 remove all elements found in list #3");
            testlist1.removeAll(testlist3);
            sender.sendMessage("§btestlist1: §e" + testlist1.size());
            sender.sendMessage("§btestlist1: §e" + String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§a#2 remove element #4");
            testlist2.remove(4);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e" + String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After remove of element nr 4: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testlist2.toArray()))));
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_LOG, logKey);
            sender.sendMessage("§a#2 retain all elements found in list #3");
            testlist2.retainAll(testlist3);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e" + String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("After retaining all elements found in #3: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testlist2.toArray()))));
            JedisCommunication.broadcast(JedisCommunicationChannel.LIST_LOG, logKey);
            sender.sendMessage("§b#3 set entry nr. 0 to element 1");
            testlist3.set(0, test1);
            sender.sendMessage("§btestlist3: §e" + testlist3.size());
            sender.sendMessage("§btestlist3: §e" + String.join(",", Arrays.toString(testlist3.toArray())));
            sender.sendMessage("");
            try (Jedis j = ClassicJedisPool.getJedis()) {
                sender.sendMessage("§bFinal values: ");
                sender.sendMessage("§btestlist1:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testlist1.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.lrange(testlist1.getKey().getData(), 0, -1)).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
                sender.sendMessage("§btestlist2:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testlist2.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.lrange(testlist2.getKey().getData(), 0, -1)).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
                sender.sendMessage("§btestlist3:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testlist3.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.lrange(testlist3.getKey().getData(), 0, -1)).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
            }
        } else if (args[0].equalsIgnoreCase("testset")) {
            if (testset1 == null) {
                testset1 = new RSet<RInteger>(new Bytes("testset1"));
            }
            if (testset2 == null) {
                testset2 = new RSet<RInteger>(new Bytes("testset2"));
            }
            if (testset3 == null) {
                testset3 = new RSet<AbstractData<?>>(new Bytes("testset3"));
            }
            if (test1 == null) {
                test1 = (RInteger) AbstractData.create(new Bytes("testint1"));
                if (test1 == null) {
                    test1 = new RInteger(new Bytes("testint1"));
                    test1.set(1);
                }
            }
            if (test2 == null) {
                test2 = (RInteger) AbstractData.create(new Bytes("testint2"));
                if (test2 == null) {
                    test2 = new RInteger(new Bytes("testint2"));
                    test2.setIfEmpty(2);
                }
            }
            if (test3 == null) {
                test3 = (RInteger) AbstractData.create(new Bytes("testint3"));
                if (test3 == null) {
                    test3 = new RInteger(new Bytes("testint3"));
                    test3.setIfEmpty(3);
                }
            }
            sender.sendMessage("§btestset1: §e" + String.join(",", Arrays.toString(testset1.toArray())));
            sender.sendMessage("§btestset2: §e" + String.join(",", Arrays.toString(testset2.toArray())));
            sender.sendMessage("§btestset3: §e" + String.join(",", Arrays.toString(testset3.toArray())));
            sender.sendMessage("§a#1,#2,#3 clear");
            testset1.clear();
            testset2.clear();
            testset3.clear();
            sender.sendMessage("§btestset1: §e" + testset1.size());
            sender.sendMessage("§a#1 add Elements 1,2,3");
            testset1.add(test1);
            testset1.add(test2);
            testset1.add(test3);
            sender.sendMessage("§btestset1: §e" + testset1.size());
            sender.sendMessage("§btestset1: §e" + String.join(",", Arrays.toString(testset1.toArray())));
            sender.sendMessage("§a#2 add Elements of set #1");
            testset2.addAll(testset1);
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After first add: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testset2.toArray()))));
            sender.sendMessage("§a#2 add Elements of set #1");
            testset2.addAll(testset1);
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After second add: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testset2.toArray()))));
            sender.sendMessage("§a#2 add Elements of set #1");
            testset2.addAll(testset1);
            sender.sendMessage("§btestset2: §e" + testset2.size());
            sender.sendMessage("§btestset2: §e" + String.join(",", Arrays.toString(testset2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After adding: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testset2.toArray()))));
            sender.sendMessage("§a#3 add Element 3");
            testset3.add(test3);
            sender.sendMessage("§btestset3: §e" + testset3.size());
            sender.sendMessage("§btestset3: §e" + String.join(",", Arrays.toString(testset3.toArray())));
            sender.sendMessage("§a#1 remove all elements found in set #3");
            testset1.removeAll(testset3);
            sender.sendMessage("§btestset1: §e" + testset1.size());
            sender.sendMessage("§btestset1: §e" + String.join(",", Arrays.toString(testset1.toArray())));
            sender.sendMessage("§a#2 remove element 3");
            testset2.remove(test3);
            sender.sendMessage("§btestset2: §e" + testset2.size());
            sender.sendMessage("§btestset2: §e" + String.join(",", Arrays.toString(testset2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG, new Bytes("After remove of element nr 4: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testset2.toArray()))));
            sender.sendMessage("§a#2 retain all elements found in set #3");
            testset2.retainAll(testset3);
            sender.sendMessage("§btestset2: §e" + testset2.size());
            sender.sendMessage("§btestset2: §e" + String.join(",", Arrays.toString(testset2.toArray())));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("After retaining all elements found in #3: "));
            JedisCommunication.broadcast(JedisCommunicationChannel.LOG,
                    new Bytes("#2 " + String.join(",", Arrays.toString(testset2.toArray()))));
            try (Jedis j = ClassicJedisPool.getJedis()) {
                sender.sendMessage("§bFinal values: ");
                sender.sendMessage("§btestset1:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testset1.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.smembers(testset1.getKey().getData())).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
                sender.sendMessage("§btestset2:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testset2.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.smembers(testset2.getKey().getData())).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
                sender.sendMessage("§btestset3:");
                sender.sendMessage("§bCache: §a" + String.join(",", Arrays.toString(testset3.toArray())));
                sender.sendMessage("§bRedis: §c"
                        + String.join(",", (j.smembers(testset3.getKey().getData())).stream()
                                .map(bytearray -> new Bytes(bytearray).asString()).toList()));
                sender.sendMessage("§bContains 1,2,3: "+String.join("§b,", Arrays.stream(new RInteger[]{test1, test2, test3}).map(element -> "§e"+(testset3.contains(element))).toList()));
                sender.sendMessage("§bContainsKey 1,2,3: "+String.join("§b,", Arrays.stream(new RInteger[]{test1, test2, test3}).map(element -> "§e"+(testset3.containsKey(element.getKey()))).toList()));
                sender.sendMessage("§bContainsValue 1,2,3: "+String.join("§b,", Arrays.stream(new RInteger[]{test1, test2, test3}).map(element -> "§e"+(testset3.containsValue(element))).toList()));
            }
        } else if (args[0].equalsIgnoreCase("testint")) {
            if (test == null) {
                test = new RInteger(new Bytes("testint"));
            }
            sender.sendMessage("§btestint: §e" + test.get());
            sender.sendMessage("§atestint++;");
            test.add(1);
            sender.sendMessage("§btestint: §e" + test.get());
        } else if (args[0].equalsIgnoreCase("testbool")) {
            if (testbool == null) {
                testbool = new RBoolean(new Bytes("testbool"));
            }
            sender.sendMessage("§btestbool: §e" + testbool.get());
            sender.sendMessage("§atestbool = !testbool");
            testbool.set(!testbool.get());
            sender.sendMessage("§btestbool: §e" + testbool.get());
        } else if (args[0].equalsIgnoreCase("testwrite")) {
            if (test == null) {
                test = new RInteger(new Bytes("testint"));
            }
            sender.sendMessage("§btestwrite:");
            Bytes m = ByteMessage.write((byte) 5);
            sender.sendMessage("§b5: §a" + ByteMessage.readIn(m, Byte.class).getValue0());
            m = ByteMessage.write((byte) 5, "testint");
            sender.sendMessage("§bMessage: " + m);
            Pair<Byte, String> t = ByteMessage.readIn(m, Byte.class, String.class);
            sender.sendMessage("§b5,testint: §a" + t.getValue0() + "§b,§a" + t.getValue1());
            m = ByteMessage.write((byte) 5, "testint",
                    new Bytes[] { new Bytes("testlist1"), new Bytes("testlist2"), new Bytes("testlist3") });
            Triplet<Byte, String, Bytes[]> tr = ByteMessage.readIn(m, Byte.class, String.class, Bytes[].class);
            Bytes[] byts = tr.getValue2();
            List<Bytes> bl = Arrays.asList(byts);
            sender.sendMessage("§b5,testint: §a" + t.getValue0() + "§b,§a" + t.getValue1() + "§b,§a"
                    + String.join("§b,§a", bl.stream().map(bytes -> bytes.asString()).toList()));
        } else if (args[0].equalsIgnoreCase("testid")) {
            if (testKeyLookupTable == null) {
                testKeyLookupTable = new KeyLookupTable(new Bytes("test"), 2);
            }
            String testname = "tobi20";
            if (args.length > 1) {
                testname = args[1];
            }
            sender.sendMessage("§bName: " + testname);
            sender.sendMessage("§bName-Bytes: " + testname.getBytes());
            Bytes id = testKeyLookupTable.lookUpId(testname);
            sender.sendMessage("§btestkeylookup (name->id) (Bytes): §e" + id);
            sender.sendMessage("§btestkeylookup (name->id): §e" + testKeyLookupTable.lookUpIdLong(testname));
            Bytes name = testKeyLookupTable.lookUpName(id);
            sender.sendMessage("§breverse-lookup (id->name)(bytes): §e" + name);
            sender.sendMessage("§breverse-lookup (id->name): §e" + name.asString());
        } else if (args[0].equalsIgnoreCase("testanon")) {
            RLong ad = dg.create(7l);
            sender.sendMessage("§btestanon: §e" + ad.getKey() + "=§a" + ad);

        } else if (args[0].equalsIgnoreCase("testfield")) {
            if (testob == null) {
                testob = new TestObject(new Bytes("testobject_new"));
            }
            sender.sendMessage("§btestob: §e" + testob.printA());
            sender.sendMessage("§atestob.switchA();");
            testob.switchA();
            sender.sendMessage("§atestob.incrA();");
            testob.incrA();
            sender.sendMessage("§btestob: §e" + testob.printA());
        } else if (args[0].equalsIgnoreCase("teststatic")) {
            sender.sendMessage("§bteststatic: §e" + TestObject.printAStatic());
            sender.sendMessage("§ateststatic.switchAStatic();");
            TestObject.switchAStatic();
            sender.sendMessage("§ateststatic.incrAStatic();");
            TestObject.incrAStatic();
            sender.sendMessage("§bteststatic: §e" + TestObject.printAStatic());
        } else if (args[0].equalsIgnoreCase("testnull")) {
            sender.sendMessage("§bnullt: §e" + TestObject.nullt);
            sender.sendMessage("§a.toggleNullt();");
            TestObject.toggleNullt();
            sender.sendMessage("§bnullt: §e" + TestObject.nullt);
        } else if (args[0].equalsIgnoreCase("testlisten")) {
            testlistener_active = !testlistener_active;
            sender.sendMessage("§bToggle testlistener " + (!testlistener_active ? "§cOFF" : "§aON"));
            if (testlistener_active) {
                AbstractData.listenGlobal(testlistener, JedisCommunicationChannel.ANY);
            } else {
                AbstractData.stopListeningGlobal(testlistener, JedisCommunicationChannel.ANY);
            }
        }  else if (args[0].equalsIgnoreCase("testreferencelisten")) {
            testreferencelistener_active = !testreferencelistener_active;
            sender.sendMessage("§bToggle test-reference-listener " + (!testreferencelistener_active ? "§cOFF" : "§aON"));
            if (testreferencelistener_active) {
                AbstractData.listenGlobal(referenceUpdateListener, JedisCommunicationChannel.REFERENCE_UPDATE);
            } else {
                AbstractData.stopListeningGlobal(referenceUpdateListener, JedisCommunicationChannel.REFERENCE_UPDATE);
            }
        } else if (args[0].equalsIgnoreCase("testow")) {
            sender.sendMessage("§boverwrite_field: §e" + TestObject.overwrite_field + " [§a"
                    + TestObject.overwrite_field.getClass().getSimpleName() + "§e]");
            sender.sendMessage("§a.overwriteField();");
            TestObject.overwriteField();
            sender.sendMessage("§boverwrite_field: §e" + TestObject.overwrite_field + " [§a"
                    + TestObject.overwrite_field.getClass().getSimpleName() + "§e]");
        } else if (args[0].equalsIgnoreCase("testchar")) {
            TestObject.testChar();
        } else if (args[0].equalsIgnoreCase("tested")) {
            TestObject.testEncodeDecode();
        } else if (args[0].equalsIgnoreCase("testasync")) {
            TestObject.testAsyncSet();
        } else if (args[0].equalsIgnoreCase("testobject")) {
            if (testob == null) {
                testob = new TestObject(new Bytes("testobject"));
            }
            sender.sendMessage("§aowners(§etestobject.c§a): §b"+testob.c.owners.stream().map(ad -> ad.getKey().toString()).collect(Collectors.joining("§a, §b")));
            sender.sendMessage("§btestob: §e" + testob);
            sender.sendMessage("§atestob.incrA();");
            testob.incrA();
            sender.sendMessage("§atestob.toggleE();");
            testob.toggleE();
            sender.sendMessage("§atestob.selectRandomTestlist();");
            testob.selectRandomTestlist();
            sender.sendMessage("§atestob.selectRandomDatatype();");
            testob.selectRandomDatatype();
            sender.sendMessage("§btestob: §e" + testob);
        } else if (args[0].equalsIgnoreCase("testenum")) {
            if (testenum == null) {
                testenum = new REnum<DataKeyPrefix>(new Bytes("testenum"), DataKeyPrefix.PLAYER);
            }
            sender.sendMessage("§btestenum.toString(): §e" + testenum.toString());
            DataKeyPrefix newValue = DataKeyPrefix.values()[(int) (Math.random() * DataKeyPrefix.values().length)];
            sender.sendMessage("§atestenum rand. new Value: " + newValue.name());
            testenum.set(newValue);
            sender.sendMessage("§btestenum: §e" + testenum);
        } else if (args[0].equalsIgnoreCase("loadis")) {
            if (Firecord.getNodeType() == NodeType.SPIGOT) {
                if (args.length == 1) {
                    sender.sendMessage("§c" + label + " loadis <player>");
                } else {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        if (testis == null) {
                            testis = new net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack(
                                    new Bytes("testis"));
                        }
                        p.getInventory().setItemInMainHand(
                                ((net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack) testis).get());
                    } else {
                        sender.sendMessage("§cCould not find player '§e" + args[1] + "§c'");
                    }
                }
            } else {
                sender.sendMessage("§cThis command is for spigot only.");
            }
        } else if (args[0].equalsIgnoreCase("storeis")) {
            if (Firecord.getNodeType() == NodeType.SPIGOT) {
                if (args.length == 1) {
                    sender.sendMessage("§c" + label + " storeis <player>");
                } else {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        if (testis == null) {
                            testis = new net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack(
                                    new Bytes("testis"));
                        }
                        ((net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack) (testis))
                                .set(p.getInventory().getItemInMainHand());
                    } else {
                        sender.sendMessage("§cCould not find player '§e" + args[1] + "§c'");
                    }
                }
            } else {
                sender.sendMessage("§cThis command is for spigot only.");
            }
        } else if (args[0].equalsIgnoreCase("redis")) {
            if (args.length == 1) {
                sender.sendMessage("§3" + ClassicJedisPool.getPoolCurrentUsage());
            } else {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    sender.sendMessage("§3Get '§e" + args[1] + "§3': §a" + j.get(args[1]));
                }
            }
        } else if (args[0].equalsIgnoreCase("bytes")) {
            if (args.length == 1) {
                sender.sendMessage("§3 Please specify a key, like 0x...");
            } else {
                try (Jedis j = ClassicJedisPool.getJedis()) {
                    sender.sendMessage("§3Get '§e" + args[1] + "§3': §a"
                            + new Bytes(j.get(Bytes.byHexString(args[1]).getData())).toString());
                }
            }
        } else if (args[0].equalsIgnoreCase("ping")) {
            if (args.length == 1) {
                sender.sendMessage("§cPlease specify a node.");
                sender.sendMessage("§c" + label + " ping <node>");
            } else {
                sender.sendMessage("§7send ping message to §e" + args[1] + "§7. See log for result.");
                if (Firecord.getNodeNames().contains(args[1])) {
                    Firecord.publish(new Bytes(args[1]),
                            JedisCommunicationChannel.PING, new Bytes(System.nanoTime()));
                } else {
                    sender.sendMessage("§cWe could not find node named \"§e" + args[1] + "§c\" in nodes: §a"
                            + String.join("§b, §a", Firecord.getNodeNames()));
                }
            }
        } else {
            sender.sendMessage("§3This subcommand isn't known... try "+label+" help");
            return false;
        }
        return true;
    }

}
