package de.minetick.modcommands;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import de.minetick.MinetickMod;

public class ReloadSettingsCommand extends Command {

    private final Logger logger = LogManager.getLogger();

    public ReloadSettingsCommand(String name) {
        super(name);
        this.usageMessage = "/minetickmod-reload";
        this.description = "Reloads the configurations of MinetickMod from its settings file minetickmod.yml";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(!sender.hasPermission("minetickmod.commands.reload")) {
            sender.sendMessage("You are not allowed to use this command!");
            return true;
        }
        this.sendMessage(sender, ChatColor.GOLD + "[MinetickMod]" + ChatColor.RESET + " Reloading config file minetickmod.yml ... ");

        FileConfiguration[] configs;

        String errorMsg = ChatColor.GOLD + "[MinetickMod]" + ChatColor.RED + " Reloading failed!\n" + ChatColor.RESET;

        try {
            configs = MinetickMod.getConfig().reload();
        } catch (IOException e) {
            this.sendMessage(sender, errorMsg + e.getMessage());
            return true;
        } catch (InvalidConfigurationException e) {
            this.sendMessage(sender, errorMsg + e.getMessage());
            return true;
        }
        FileConfiguration old = configs[0];
        FileConfiguration current = configs[1];

        StringBuilder sb = new StringBuilder();
        for(String key: old.getKeys(true)) {
            String v1 = old.getString(key);
            String v2 = current.getString(key);
            if(v1 != null && v2 == null) {
                sb.append(ChatColor.RED);
                sb.append("Removed entry: ");
                sb.append(ChatColor.RESET);
                sb.append(key);
                sb.append("\nValue: ");
                sb.append(v1);
                sb.append("\n");
            }
        }
        for(String key: current.getKeys(true)) {
            String v1 = old.getString(key);
            String v2 = current.getString(key);
            if((v1 == null || v1.isEmpty()) && v2 != null) {
                sb.append(ChatColor.YELLOW);
                sb.append("New entry: ");
                sb.append(ChatColor.RESET);
                sb.append(key);
                sb.append("\nValue: ");
                sb.append(v2);
                sb.append("\n");
            } else if(v1 != null && v2 != null && !v1.equals(v2)) {
                sb.append(ChatColor.GREEN);
                sb.append("Changed entry: ");
                sb.append(ChatColor.RESET);
                sb.append(key);
                sb.append("\nFrom: ");
                sb.append(v1);
                sb.append("\nTo: ");
                sb.append(v2);
                sb.append("\n");
            }
        }
        if (sb.length() > 0) {
            this.sendMessage(sender, sb.toString());
        }
        this.sendMessage(sender, ChatColor.GOLD + "[MinetickMod]" + ChatColor.RESET + " Config file has been reloaded.");
        return true;
    }

    private void sendMessage(CommandSender receiver, String msg) {
        this.logger.info(ChatColor.stripColor(msg));
        if(!(receiver instanceof ConsoleCommandSender)) {
            receiver.sendMessage(msg);
        }
    }
}