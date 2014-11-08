package de.minetick.modcommands;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldServer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.minetick.MinetickMod;
import de.minetick.profiler.Profile;
import de.minetick.profiler.WorldProfile;

public class WorldStatsCommand extends Command {

    private WorldNameComparator comparator = new WorldNameComparator();

    public WorldStatsCommand(String name) {
        super(name);
        this.usageMessage = "/worldstats";
        this.description = "Displays technically important details about the active worlds";
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if(!sender.isOp()) {
            sender.sendMessage("You are not allowed to use this command!");
            return true;
        }
        if(args.length < 2) {
            boolean allWorlds = args.length == 0;
            boolean foundWorld = allWorlds;
            boolean sentHeader = false;
            List<WorldServer> worlds = new LinkedList<WorldServer>();
            worlds.addAll(MinecraftServer.getServer().worlds);
            Collections.sort(worlds, this.comparator);
            InfoHolder overall = null;
            if(allWorlds) { overall = new InfoHolder("TOTAL"); }

            for(WorldServer ws: worlds) {
                boolean readWorld = allWorlds;
                if(!allWorlds) {
                    String name = ws.getWorld().getName();
                    if(name.equalsIgnoreCase(args[0])) {
                        readWorld = true;
                        foundWorld = true;
                    }
                }
                if(readWorld) {
                    if(!sentHeader) {
                        sender.sendMessage("[MinetickMod]" + ChatColor.GOLD + " WorldStats:");
                        sender.sendMessage(ChatColor.GRAY + "Name " + ChatColor.YELLOW + "Chunks " + ChatColor.GREEN + "Entites " +
                                           ChatColor.BLUE + "TileEntities " + ChatColor.DARK_PURPLE + "Players " + ChatColor.RED + "TickTime");
                        sentHeader = true;
                    }
                    InfoHolder worldDetails = this.readWorldDetails(ws);
                    if(allWorlds) { overall.add(worldDetails); }
                    WorldProfile worldProfile = MinetickMod.getProfilerStatic().getWorldProfile(ws.getWorld().getName());
                    sender.sendMessage(ChatColor.GRAY + worldDetails.name + "  " +
                                       ChatColor.YELLOW + worldDetails.chunks + ChatColor.RED + "(" + worldProfile.doTick.getLastAvgFloat() + "|" + worldProfile.chunkLoading.getLastAvgFloat() + ")  " + 
                                       ChatColor.GREEN + worldDetails.entities + ChatColor.RED + "(" + worldProfile.tickEntities.getLastAvgFloat() + ")  " +
                                       ChatColor.BLUE + worldDetails.tileEntities + ChatColor.RED + "(" + worldProfile.tickTileEntities.getLastAvgFloat() + ")  " +
                                       ChatColor.DARK_PURPLE + worldDetails.players + ChatColor.RED + "(" + worldProfile.updatePlayers.getLastAvgFloat() + ")  " +
                                       ChatColor.RED + "  " + worldProfile.getLastAvgFloat() + "ms");
                }
            }
            if(allWorlds) {
                sender.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + overall.name + " " + ChatColor.RESET +
                        ChatColor.YELLOW + overall.chunks + "  " +
                        ChatColor.GREEN + overall.entities + "  " +
                        ChatColor.BLUE + overall.tileEntities + "  " +
                        ChatColor.DARK_PURPLE + overall.players);
            } else if(!foundWorld) {
                sender.sendMessage("World '" + args[0] + "' was not found!");
                return false;
            }
            worlds.clear();
        } else {
            sender.sendMessage("Command usage: /worldstats [worldName]");
            return false;
        }
        return true;
    }

    private class WorldNameComparator implements Comparator<WorldServer> {
        @Override
        public int compare(WorldServer arg0, WorldServer arg1) {
            String n1 = arg0.getWorld().getName();
            String n2 = arg1.getWorld().getName();
            return n1.compareToIgnoreCase(n2);
        }
    }

    private class InfoHolder {
        public String name;
        public int chunks = 0;
        public int entities = 0;
        public int tileEntities = 0;
        public int players = 0;
        public float tickTime = 0.0F;

        public InfoHolder(String name) {
            this.name = name;
        }

        public void add(InfoHolder ih) {
            this.chunks += ih.chunks;
            this.entities += ih.entities;
            this.tileEntities += ih.tileEntities;
            this.players += ih.players;
            this.tickTime += ih.tickTime;
        }
    }

    private InfoHolder readWorldDetails(WorldServer ws) {
        InfoHolder ih = new InfoHolder(ws.getWorld().getName());
        ih.chunks = ws.chunkProviderServer.chunks.size();
        ih.entities = ws.entityList.size();
        ih.tileEntities = ws.tileEntityList.size();
        ih.players = ws.players.size();
        ih.tickTime = ((float)(ws.getLastTickAvg() / 100000L)) / 10.0F;
        return ih;
    }

}
