package de.minetick.modcommands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordinates;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.LongHash;

public class LoadedChunksCommand extends Command {

    public LoadedChunksCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender sender, String arg1, String[] args) {
        if(sender.isOp()) {
            if(args.length == 1) {
                String world = args[0];
                World w = Bukkit.getServer().getWorld(world);
                if(w != null) {
                    CraftWorld cw = (CraftWorld) w;
                    WorldServer ws = cw.getHandle();
                    Collection<Chunk> set = ws.chunkProviderServer.chunks.values();
                    ChunkCoordinates cc = ws.getSpawn();

                    List<ChunkCoordIntPair> list = new ArrayList<ChunkCoordIntPair>();
                    short short1 = 128;
                    for(Chunk chunk: set) {
                        int k = chunk.locX * 16 + 8 - cc.x;
                        int l = chunk.locZ * 16 + 8 - cc.z;
    
                        boolean isNotInSpawn = (k < -short1 || k > short1 || l < -short1 || l > short1 || !(ws.keepSpawnInMemory));
                        boolean inUse = w.isChunkInUse(chunk.locX, chunk.locZ);
                        if(isNotInSpawn && !inUse) {
                            list.add(chunk.l());
                        }
                    }
                    String out = "";
                    for(ChunkCoordIntPair ccip: list) {
                        out += ("[" + ccip.x + "," + ccip.z + "],");
                    }
                    sender.sendMessage("[MinetickMod] Loaded chunks in world " + w.getName() + " (except spawn and player areas):");
                    sender.sendMessage(out);
                } else {
                    sender.sendMessage("[MinetickMod] World " + world + " not found!");
                }
            } else {
                sender.sendMessage("[MinetickMod] Usage: /loadedchunks <world name>");
            }
        } else {
            sender.sendMessage("[MinetickMod] You dont have the permission to use this command");
        }
        return true;
    }
}
