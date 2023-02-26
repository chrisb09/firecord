package net.legendofwar.firecord.spigot;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.tool.NodeType;

public final class FirecordSpigot extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {

        Bukkit.getLogger().info("Firecord - Spigot-Start registred.");

        String[] pathSplit = getDataFolder().getAbsolutePath().split("/");
        String serverName = pathSplit[pathSplit.length - 3];
        Bukkit.getLogger().info("ServerName: " + serverName);

        if (Firecord.init(serverName, NodeType.SPIGOT)) {
            this.getCommand("firecord").setExecutor(new FirecordCommand());
        } else {
            Bukkit.getPluginManager().disablePlugin(this);
        }

    }

}
