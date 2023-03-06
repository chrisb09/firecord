package net.legendofwar.firecord.communication;

public interface MessageReceiver {
    
    public void receive(String channel, String sender, boolean broadcast, String message);

}
