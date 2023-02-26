package net.legendofwar.firecord.spigot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FirecordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§b"+label+" id    §e show id of this node");
            sender.sendMessage("§b"+label+" help  §e show this help page");
        }
        return true;
    }
    
}
