package net.legendofwar.firecord.standalone;

import java.util.Arrays;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import net.legendofwar.firecord.command.CliSender;
import net.legendofwar.firecord.command.FirecordCommand;

public class Cli {

    // In this class we will handle command line arguments for the standalone version of Firecord
    // On paper/velcity, we used /firecord or /fc as the cmd, in standalone we do not need that prefix and instead the subcommands can be used directly in the terminal
    // However, we need some quality of life terminal/cli features like:
    // -input lines start with an $
    // -tab completion for subcommands and arguments
    // the firecordcommand's commands will be used as the base for this, they will also be what's executed
    // we will also need to handle invalid commands and arguments, and provide help messages
    // we cant directly write what firecordcommand does to the terminal, so we will need to modify the clisender class to handle that - especiall the color codes and minecraft formatting that needs to be converted to what the terminal can display
    // we want the additional logic to close the cli when the user types "exit" or "quit" or "close" presses ctrl+c (properly handle the shutdown hook)
    
    private static final CliSender sender = new CliSender();
    private static volatile boolean running = true;
    
    // Available commands for tab completion
    private static final List<String> COMMANDS = Arrays.asList(
        "help", "id", "ids", "list", "test", "testid", "testint", "loadis", "storeis",
        "bytes", "testfield", "teststatic", "testchar", "testbool", "testnull",
        "testlisten", "testreferencelisten", "testow", "testasync", "tested",
        "testlist", "testset", "testmap", "testanon", "tobytes", "listclasses",
        "testmessage", "testobject", "testenum", "ping", "redis", "exit", "quit", "close"
    );
    
    public static void run() {
        // Setup shutdown hook for graceful exit on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (running) {
                System.out.println("\n\u001B[93mShutting down Firecord CLI...\u001B[0m");
                running = false;
            }
        }));
        
        try {
            // Create JLine terminal and reader for proper command line handling
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new StringsCompleter(COMMANDS))
                    .build();
            
            // Welcome message
            sender.sendMessage("§b========================================");
            sender.sendMessage("§b    Welcome to Firecord Standalone CLI");
            sender.sendMessage("§b========================================");
            sender.sendMessage("§eType '§bhelp§e' for available commands");
            sender.sendMessage("§eType '§bexit§e', '§bquit§e', or '§bclose§e' to exit");
            sender.sendMessage("§7Press Ctrl+C to exit anytime");
            sender.sendMessage("§7Use ↑/↓ arrow keys for command history, Tab for completion");
            sender.sendMessage("§7Note: Redis-dependent commands may fail if Redis is not available");
            sender.sendMessage("");
            
            while (running) {
                try {
                    // Read line with JLine (supports history, completion, etc.)
                    String input = reader.readLine("\u001B[96m$\u001B[0m ");
                    
                    if (input == null) {
                        // EOF (Ctrl+D)
                        break;
                    }
                    
                    input = input.trim();
                    if (input.isEmpty()) {
                        continue;
                    }
                    
                    // Handle exit commands
                    if (isExitCommand(input)) {
                        sender.sendMessage("§eGoodbye!");
                        break;
                    }
                    
                    // Parse command and arguments
                    String[] parts = input.split("\\s+");
                    String command = parts[0].toLowerCase();
                    String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                    
                    // Create the full args array including the command as the first argument
                    String[] fullArgs = new String[args.length + 1];
                    fullArgs[0] = command;
                    System.arraycopy(args, 0, fullArgs, 1, args.length);
                    
                    // Execute command through FirecordCommand
                    try {
                        // Use empty label since we don't need command prefix in standalone CLI
                        boolean commandFound = FirecordCommand.onCommand(sender, "", fullArgs);
                        if (!commandFound) {
                            sender.sendMessage("§cUnknown command: §e" + command);
                            sender.sendMessage("§7Type '§bhelp§7' for available commands");
                        }
                    } catch (Exception e) {
                        sender.sendMessage("§cError executing command: §e" + e.getMessage());
                        sender.sendMessage("§7This might be due to Redis not being available or other missing dependencies");
                        if (command.equals("help")) {
                            // If even help fails, show basic help
                            showBasicHelp();
                        }
                    }
                    
                } catch (org.jline.reader.UserInterruptException e) {
                    // Ctrl+C pressed
                    sender.sendMessage("\n§eGoodbye!");
                    break;
                } catch (org.jline.reader.EndOfFileException e) {
                    // Ctrl+D pressed
                    break;
                } catch (Exception e) {
                    sender.sendMessage("§cAn unexpected error occurred: §e" + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            terminal.close();
        } catch (Exception e) {
            sender.sendMessage("§cFailed to initialize terminal: §e" + e.getMessage());
            sender.sendMessage("§7Falling back to basic input mode...");
            runBasicMode();
        }
        
        running = false;
    }
    
    // Fallback method using Scanner if JLine fails
    private static void runBasicMode() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        
        sender.sendMessage("§7Running in basic mode (no history/completion)");
        
        while (running) {
            try {
                System.out.print("\u001B[96m$\u001B[0m ");
                
                if (!scanner.hasNextLine()) {
                    break;
                }
                
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                if (isExitCommand(input)) {
                    sender.sendMessage("§eGoodbye!");
                    break;
                }
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                
                String[] fullArgs = new String[args.length + 1];
                fullArgs[0] = command;
                System.arraycopy(args, 0, fullArgs, 1, args.length);
                
                try {
                    boolean commandFound = FirecordCommand.onCommand(sender, "", fullArgs);
                    if (!commandFound) {
                        sender.sendMessage("§cUnknown command: §e" + command);
                        sender.sendMessage("§7Type '§bhelp§7' for available commands");
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cError executing command: §e" + e.getMessage());
                }
                
            } catch (Exception e) {
                sender.sendMessage("§cAn unexpected error occurred: §e" + e.getMessage());
                e.printStackTrace();
            }
        }
        
        scanner.close();
    }
    
    private static void showBasicHelp() {
        sender.sendMessage("§b=== Basic Commands ===");
        sender.sendMessage("§bhelp        §e show this help page");
        sender.sendMessage("§bexit/quit   §e exit the CLI");
        sender.sendMessage("§7Note: Some commands require Redis to be running");
    }
    
    private static boolean isExitCommand(String input) {
        String command = input.toLowerCase().trim();
        return command.equals("exit") || command.equals("quit") || command.equals("close");
    }
    
    // Method to get command suggestions for tab completion (for future enhancement)
    public static List<String> getCommandSuggestions(String partial) {
        return COMMANDS.stream()
                .filter(cmd -> cmd.startsWith(partial.toLowerCase()))
                .toList();
    }
}
