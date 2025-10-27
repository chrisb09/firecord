package net.legendofwar.firecord.command;

import java.util.HashMap;
import java.util.Map;

public class CliSender implements Sender {

    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    
    static {
        // Minecraft color codes to ANSI escape sequences
        COLOR_MAP.put("§0", "\u001B[30m");    // Black
        COLOR_MAP.put("§1", "\u001B[34m");    // Dark Blue
        COLOR_MAP.put("§2", "\u001B[32m");    // Dark Green
        COLOR_MAP.put("§3", "\u001B[36m");    // Dark Aqua
        COLOR_MAP.put("§4", "\u001B[31m");    // Dark Red
        COLOR_MAP.put("§5", "\u001B[35m");    // Dark Purple
        COLOR_MAP.put("§6", "\u001B[33m");    // Gold
        COLOR_MAP.put("§7", "\u001B[37m");    // Gray
        COLOR_MAP.put("§8", "\u001B[90m");    // Dark Gray
        COLOR_MAP.put("§9", "\u001B[94m");    // Blue
        COLOR_MAP.put("§a", "\u001B[92m");    // Green
        COLOR_MAP.put("§b", "\u001B[96m");    // Aqua
        COLOR_MAP.put("§c", "\u001B[91m");    // Red
        COLOR_MAP.put("§d", "\u001B[95m");    // Light Purple
        COLOR_MAP.put("§e", "\u001B[93m");    // Yellow
        COLOR_MAP.put("§f", "\u001B[97m");    // White
        COLOR_MAP.put("§r", "\u001B[0m");     // Reset
        
        // Formatting codes
        COLOR_MAP.put("§l", "\u001B[1m");     // Bold
        COLOR_MAP.put("§m", "\u001B[9m");     // Strikethrough
        COLOR_MAP.put("§n", "\u001B[4m");     // Underline
        COLOR_MAP.put("§o", "\u001B[3m");     // Italic
    }

    @Override
    public void sendMessage(String message) {
        String convertedMessage = convertMinecraftToAnsi(message);
        System.out.println(convertedMessage + "\u001B[0m"); // Always reset at the end
    }
    
    private String convertMinecraftToAnsi(String message) {
        String result = message;
        for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
