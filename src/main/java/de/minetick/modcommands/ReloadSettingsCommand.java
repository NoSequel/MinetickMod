package de.minetick.modcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.minetick.MinetickMod;

public class ReloadSettingsCommand extends Command {

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
        sender.sendMessage("[MinetickMod] Reloading config file minetickmod.yml ...");
        MinetickMod.getConfig().reload();
        sender.sendMessage("[MinetickMod] Config file has been reloaded.");
        return true;
    }
}
