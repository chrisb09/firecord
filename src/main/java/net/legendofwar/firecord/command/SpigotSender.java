package net.legendofwar.firecord.command;

import org.bukkit.command.CommandSender;

public class SpigotSender implements Sender {

    private CommandSender sender;

    public SpigotSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

}
