package net.legendofwar.firecord.command;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInt;
import net.legendofwar.firecord.tool.NodeType;
import redis.clients.jedis.Jedis;

public class FirecordCommand {

    static RInt test = null;
    static Object testis = null;

    public static boolean onCommand(Sender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b" + label + " id          §e show id of this node");
            sender.sendMessage("§b" + label + " ids/list    §e show ids of all nodes");
            sender.sendMessage("§b" + label + " test        §e broadcast test message");
            sender.sendMessage("§b" + label + " testint     §e small data sync example");
            sender.sendMessage("§b" + label + " loadis      §e large data sync example");
            sender.sendMessage("§b" + label + " storeis      §e large data sync example");
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
        } else if (args[0].equalsIgnoreCase("testint")) {
            if (test == null) {
                test = new RInt("testint", 0);
            }
            sender.sendMessage("§btestint: §e" + test.get());
            sender.sendMessage("§atestint++;");
            test.add(1);
            sender.sendMessage("§btestint: §e" + test.get());
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
