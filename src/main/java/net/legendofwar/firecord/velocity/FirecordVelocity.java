package net.legendofwar.firecord.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.command.FirecordCommand;
import net.legendofwar.firecord.command.VelocitySender;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.tool.NodeType;

import java.nio.file.Path;

import org.slf4j.Logger;


@Plugin(id = "firecord", name = "Firecord", version = "1.1",
        url = "https://legendofwar.net", description = "-", authors = {"tobi20"})
public class FirecordVelocity {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public FirecordVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;

        getLogger().info("Firecord - Velocity-Start registred.");

        String[] pathSplit = dataDirectory.toAbsolutePath().toString().split("/");
        String serverName = pathSplit[pathSplit.length - 3];
        getLogger().info("ServerName: " + serverName);

        Firecord.init(new Bytes(serverName), NodeType.VELOCITY);
    }

    public Logger getLogger(){
        return logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("onProxyInitialization called!");
        CommandManager commandManager = server.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("firecord")
            .plugin(this)
            .build();


        RawCommand commandToRegister = new RawCommand() {

            @Override
            public void execute(Invocation arg0) {
                FirecordCommand.onCommand(new VelocitySender(arg0.source()), "firecord", arg0.arguments().split(" "));
            }

            @Override
            public boolean hasPermission(final Invocation invocation) {
                return true;
                //return invocation.source().hasPermission("firecord");
            }
            
        };
        // Finally, you can register the command
        commandManager.register(commandMeta, commandToRegister);

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event){
        Firecord.disable();
    }

}