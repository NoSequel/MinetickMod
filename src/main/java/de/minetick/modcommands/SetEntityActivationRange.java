package de.minetick.modcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.minetick.MinetickMod;

public class SetEntityActivationRange extends Command {

    public SetEntityActivationRange(String name) {
        super(name);
        this.usageMessage = "/setentityactivationrange <Int:low> <Int:high> [Int:max]";
        this.description = "Sets the lower and higher activation range for un-important entities";
    }

    @Override
    public boolean execute(CommandSender sender, String arg1, String[] args) {
        if(sender.isOp()) {
            if(args.length >= 2 && args.length <= 3) {
                int low = 0, high = 0, max = -1;
                boolean error = false;
                try {
                    low = Integer.parseInt(args[0]);
                } catch(NumberFormatException e) {
                    error = true;
                }
                try {
                    high = Integer.parseInt(args[1]);
                } catch(NumberFormatException e) {
                    error = true;
                }
                if(args.length == 3) {
                    try {
                        max = Integer.parseInt(args[2]);
                    } catch(NumberFormatException e) {
                        error = true;
                    }
                }
                if(!error) {
                    if(!MinetickMod.setActivationRange(low, high, max)) {
                        sender.sendMessage("Invalid min/max values.   4 < min < 144   and   min <= max < 144");
                    } else {
                        String maxStr = "";
                        if(max > 0) {
                            maxStr = "   Max: " + max;
                        }
                        sender.sendMessage("New activation ranges set:   Low: " + low + "   High: " + high + maxStr);
                    }
                } else {
                    sender.sendMessage("Invalid arguments. Usage: /setentityactivationrange <Int:low> <Int:high> [Int:max]");
                }
            } else {
                sender.sendMessage("Usage: /setentityactivationrange <Int:low> <Int:high> [Int:max]");
            }
        } else {
            sender.sendMessage("You dont have the permission to use this command.");
        }
        return true;
    }
}
