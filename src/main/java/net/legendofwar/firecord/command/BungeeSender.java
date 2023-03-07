package net.legendofwar.firecord.command;

import net.md_5.bungee.api.CommandSender;

public class BungeeSender implements Sender {

    private CommandSender sender;

    public BungeeSender(CommandSender sender) {
        this.sender = sender;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

}
