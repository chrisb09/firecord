package net.legendofwar.firecord.spigot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.legendofwar.firecord.Firecord;

public class FirecordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b" + label + " id    §e show id of this node");
            sender.sendMessage("§b" + label + " ids/list   §e show ids of all nodes");
            sender.sendMessage("§b" + label + " test  §e broadcast test message");
            sender.sendMessage("§b" + label + " ping <node> §e broadcast test message");
            sender.sendMessage("§b" + label + " help  §e show this help page");
        } else if (args[0].equalsIgnoreCase("id")) {
            sender.sendMessage("§bid: §e" + Firecord.getId());
        } else if (args[0].equalsIgnoreCase("ids") || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§bids: §e" + String.join(", ", Firecord.getNodes()));
        } else if (args[0].equalsIgnoreCase("test")) {
            sender.sendMessage("§7send broadcast message to all other nodes that causes a entry in the log.");
            Firecord.broadcast("test", "Hello World");
        } else if (args[0].equalsIgnoreCase("ping")) {
            if (args.length == 1) {
                sender.sendMessage("§cPlease specify a node.");
                sender.sendMessage("§c" + label + " ping <node>");
            } else {
                sender.sendMessage("§7send ping message to §e" + args[1] + "§7. See log for result.");
                Firecord.publish(args[1], "ping", "" + System.nanoTime());
            }
        }
        return true;
    }

}
