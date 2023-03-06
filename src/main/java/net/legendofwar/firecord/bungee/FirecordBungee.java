package net.legendofwar.firecord.bungee;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.tool.NodeType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class FirecordBungee extends Plugin {
    
    @Override
    public void onEnable() {

        getLogger().info("Firecord - Spigot-Start registred.");

        String[] pathSplit = getDataFolder().getAbsolutePath().split("/");
        String serverName = pathSplit[pathSplit.length - 3];
        getLogger().info("ServerName: " + serverName);

        Firecord.init(serverName, NodeType.BUNGEE);

        net.md_5.bungee.api.ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command("firecord") {

            @SuppressWarnings("deprecation")
            @Override
            public void execute(CommandSender sender, String[] args) {
                String label = "/firecord";
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
            }
            
        });

    }

    @Override
    public void onDisable() {
        Firecord.disable();
    }
    
}
