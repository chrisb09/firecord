package net.legendofwar.firecord.command;

import java.util.Arrays;
import java.util.Base64;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.composite.RList;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.REnum;
import net.legendofwar.firecord.jedis.dataset.dataentry.object.TestObject;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.tool.NodeType;
import redis.clients.jedis.Jedis;

public class FirecordCommand {

    static RInteger test = null;
    static RInteger test1 = null;
    static RInteger test2 = null;
    static RInteger test3 = null;
    static Object testis = null;
    static TestObject testob = null;
    static REnum<DataType> testenum = null;
    static RList<RInteger> testlist1 = null;
    static RList<RInteger> testlist2 = null;
    static RList<AbstractData<?>> testlist3 = null;

    public static boolean onCommand(Sender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b" + label + " id          §e show id of this node");
            sender.sendMessage("§b" + label + " ids/list    §e show ids of all nodes");
            sender.sendMessage("§b" + label + " test        §e broadcast test message");
            sender.sendMessage("§b" + label + " testint     §e small data sync example");
            sender.sendMessage("§b" + label + " loadis      §e large data sync example");
            sender.sendMessage("§b" + label + " storeis     §e large data sync example");
            sender.sendMessage("§b" + label + " testlist    §e list test command");
            sender.sendMessage("§b" + label + " testanon    §e anonymous type test command");
            sender.sendMessage("§b" + label + " testobject  §e test object command");
            sender.sendMessage("§b" + label + " testenum    §e construct an enum");
            sender.sendMessage("§b" + label + " ping <node> §e broadcast test message");
            sender.sendMessage("§b" + label + " redis <key> §e get redis entry at key");
            sender.sendMessage("§b" + label + " help        §e show this help page");
        } else if (args[0].equalsIgnoreCase("id")) {
            sender.sendMessage("§bid: §e" + Firecord.getId());
        } else if (args[0].equalsIgnoreCase("ids") || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§bids: §e" + String.join(", ", Firecord.getNodes()));
        } else if (args[0].equalsIgnoreCase("test")) {
            sender.sendMessage("§7send broadcast message to all other nodes that causes a entry in the log.");
            Firecord.broadcast("test", "Hello World");
        } else if (args[0].equalsIgnoreCase("serialize")) {
            if (test == null) {
                test = new RInteger("testint", 0);
            }
        } else if (args[0].equalsIgnoreCase("testlist")) {
            if (testlist1 == null) {
                testlist1 = new RList<RInteger>("testlist1");
            }
            if (testlist2 == null) {
                testlist2 = new RList<RInteger>("testlist2");
            }
            if (testlist3 == null) {
                testlist3 = new RList<AbstractData<?>>("testlist3");
            }
            if (test1 == null) {
                test1 = (RInteger) AbstractData.create("testint1");
                if (test1 == null){
                    test1 = new RInteger("testint1", 1);
                }
            }
            if (test2 == null) {
                test2 = (RInteger) AbstractData.create("testint2");
                if (test2 == null){
                    test2 = new RInteger("testint2", 2);
                }
            }
            if (test3 == null) {
                test3 = (RInteger) AbstractData.create("testint3");
                if (test3 == null){
                    test3 = new RInteger("testint3", 3);
                }
            }
            String logKey = new String(Base64.getEncoder().encode(testlist2.getKey().getBytes()));
            sender.sendMessage("§btestlist1: §e"+String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§btestlist2: §e"+String.join(",", Arrays.toString(testlist2.toArray())));
            sender.sendMessage("§btestlist3: §e"+String.join(",", Arrays.toString(testlist3.toArray())));
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
            sender.sendMessage("§btestlist1: §e"+String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§a#2 add Elements of list #1");
            testlist2.addAll(testlist1);
            JedisCommunication.broadcast("log", "After first add: ");
            JedisCommunication.broadcast("log", "#2 "+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("RList_log", logKey);
            sender.sendMessage("§a#2 add Elements of list #1 at position 1");
            testlist2.addAll(1, testlist1);
            JedisCommunication.broadcast("log", "After second add: ");
            JedisCommunication.broadcast("log", "#2 "+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("RList_log", logKey);
            sender.sendMessage("§a#2 add Elements of list #1 at position 3");
            testlist2.addAll(3, testlist1);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e"+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("log", "After adding: ");
            JedisCommunication.broadcast("log", "#2 "+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("RList_log", logKey);
            sender.sendMessage("§a#3 add Element 3");
            testlist3.add(test3);
            sender.sendMessage("§btestlist3: §e" + testlist3.size());
            sender.sendMessage("§btestlist3: §e"+String.join(",", Arrays.toString(testlist3.toArray())));
            sender.sendMessage("§a#1 remove all elements found in list #3");
            testlist1.removeAll(testlist3);
            sender.sendMessage("§btestlist1: §e" + testlist1.size());
            sender.sendMessage("§btestlist1: §e"+String.join(",", Arrays.toString(testlist1.toArray())));
            sender.sendMessage("§a#2 remove element #4");
            testlist2.remove(4);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e"+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("log", "After remove of element nr 4: ");
            JedisCommunication.broadcast("log", "#2 "+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("RList_log", logKey);
            sender.sendMessage("§a#2 retain all elements found in list #3");
            testlist2.retainAll(testlist3);
            sender.sendMessage("§btestlist2: §e" + testlist2.size());
            sender.sendMessage("§btestlist2: §e"+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("log", "After retaining all elements found in #3: ");
            JedisCommunication.broadcast("log", "#2 "+String.join(",", Arrays.toString(testlist2.toArray())));
            JedisCommunication.broadcast("RList_log", logKey);
            sender.sendMessage("§b#3 set entry nr. 0 to element 1");
            testlist3.set(0, test1);
            sender.sendMessage("§btestlist3: §e" + testlist3.size());
            sender.sendMessage("§btestlist3: §e"+String.join(",", Arrays.toString(testlist3.toArray())));
            sender.sendMessage("");
            try (Jedis j = ClassicJedisPool.getJedis()) {
                sender.sendMessage("§bFinal values: ");
                sender.sendMessage("§btestlist1:");
                sender.sendMessage("§bCache: §a"+String.join(",", Arrays.toString(testlist1.toArray())));
                sender.sendMessage("§bRedis: §c"+String.join(",", Arrays.toString(j.lrange(testlist1.getKey(), 0, -1).toArray())));
                sender.sendMessage("§btestlist2:");
                sender.sendMessage("§bCache: §a"+String.join(",", Arrays.toString(testlist2.toArray())));
                sender.sendMessage("§bRedis: §c"+String.join(",", Arrays.toString(j.lrange(testlist2.getKey(), 0, -1).toArray())));
                sender.sendMessage("§btestlist3:");
                sender.sendMessage("§bCache: §a"+String.join(",", Arrays.toString(testlist3.toArray())));
                sender.sendMessage("§bRedis: §c"+String.join(",", Arrays.toString(j.lrange(testlist3.getKey(), 0, -1).toArray())));
            }
        } else if (args[0].equalsIgnoreCase("testint")) {
            if (test == null) {
                test = new RInteger("testint", 0);
            }
            sender.sendMessage("§btestint: §e" + test.get());
            sender.sendMessage("§atestint++;");
            test.add(1);
            sender.sendMessage("§btestint: §e" + test.get());
        }  else if (args[0].equalsIgnoreCase("testanon")) {
            AbstractData<?> ad = DataPool.createAnonymous(DataType.DOUBLE);
            sender.sendMessage("§btestanon: §e" + ad);
        }  else if (args[0].equalsIgnoreCase("testobject")) {
            if (testob == null) {
                testob = new TestObject("testobject");
            }
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
                testenum = new REnum<DataType>("testenum", DataType.STRING);
            }
            sender.sendMessage("§btestenum.toString(): §e" + testenum.toString());
            DataType newValue = DataType.values()[(int) (Math.random()*DataType.values().length)];
            sender.sendMessage("§atestenum rand. new Value: "+newValue.name());
            testenum.set(newValue);
            sender.sendMessage("§btestenum: §e" + testenum);
        }  else if (args[0].equalsIgnoreCase("loadis")) {
            if (Firecord.getNodeType() == NodeType.SPIGOT){
                if (args.length==1) {
                    sender.sendMessage("§c"+label+" loadis <player>");
                } else {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        if (testis == null) {
                            testis = new net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack("testis", new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE) );   
                        }
                        p.getInventory().setItemInMainHand(((net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack) testis).get());
                    }else{
                        sender.sendMessage("§cCould not find player '§e"+args[1]+"§c'");   
                    }
                }
            } else {
                sender.sendMessage("§cThis command is for spigot only.");
            }
        }   else if (args[0].equalsIgnoreCase("storeis")) {
            if (Firecord.getNodeType() == NodeType.SPIGOT){
                if (args.length==1) {
                    sender.sendMessage("§c"+label+" storeis <player>");
                } else {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (p != null) {
                        if (testis == null) {
                            testis = new net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack("testis", new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE) );   
                        }
                        ((net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack) (testis)).set(p.getInventory().getItemInMainHand());
                    }else{
                        sender.sendMessage("§cCould not find player '§e"+args[1]+"§c'");   
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
        } else if (args[0].equalsIgnoreCase("ping")) {
            if (args.length == 1) {
                sender.sendMessage("§cPlease specify a node.");
                sender.sendMessage("§c" + label + " ping <node>");
            } else {
                sender.sendMessage("§7send ping message to §e" + args[1] + "§7. See log for result.");
                Firecord.publish(args[1], "ping", "" + System.nanoTime());
            }
        } else {
            sender.sendMessage("§3This subcommand isn't known...");
            return false;
        }
        return true;
    }

}
