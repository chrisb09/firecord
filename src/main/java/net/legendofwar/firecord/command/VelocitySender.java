package net.legendofwar.firecord.command;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;

public class VelocitySender implements Sender {

    private CommandSource source;

    public VelocitySender(CommandSource source){
        this.source = source;
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(Component.text(message));
    }

}
