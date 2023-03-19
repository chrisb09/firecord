package net.legendofwar.firecord.bungee;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.command.BungeeSender;
import net.legendofwar.firecord.command.FirecordCommand;
import net.legendofwar.firecord.tool.NodeType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class FirecordBungee extends Plugin {

    @Override
    public void onEnable() {

        getLogger().info("Firecord - Bungeecord-Start registred.");

        String[] pathSplit = getDataFolder().getAbsolutePath().split("/");
        String serverName = pathSplit[pathSplit.length - 3];
        getLogger().info("ServerName: " + serverName);

        Firecord.init(serverName, NodeType.BUNGEE);

        net.md_5.bungee.api.ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command("firecord") {

            @Override
            public void execute(CommandSender sender, String[] args) {
                String label = "/firecord";
                FirecordCommand.onCommand(new BungeeSender(sender), label, args);
            }

        });

    }

    @Override
    public void onDisable() {
        Firecord.disable();
    }

}
