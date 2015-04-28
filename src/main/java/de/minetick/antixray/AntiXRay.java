package de.minetick.antixray;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.World.Environment;

import net.minecraft.server.Block;
import net.minecraft.server.Blocks;
import net.minecraft.server.Chunk;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

public class AntiXRay {

    private boolean enabled = false;
    private static boolean blocksToHide[] = new boolean[65536];
    private static int[][] additionalUpdatePositions = new int[][]{ { 2,0, 0},{ 2,0, 1},{ 2,0,-1},
                                                                  {-2,0, 0},{-2,0, 1},{-2,0,-1},
                                                                  { 0,0,-2},{ 1,0,-2},{-1,0,-2},
                                                                  { 0,0, 2},{ 1,0, 2},{-1,0, 2},
                                                                  { 0,-2,0},{ 0,2, 0},{ 3,0, 0},
                                                                  {-3, 0,0},{ 0,0, 3},{ 0,0,-3}};
    private static List<String> configWorlds = new LinkedList<String>();
    private Random random = new Random();

    public static void setWorldsFromConfig(List<String> list) {
        configWorlds.clear();
        configWorlds.addAll(list);
    }

    public boolean isNether(WorldServer world) {
        return world.getWorld().getEnvironment().equals(Environment.NETHER);
    }

    public boolean isOverworld(WorldServer world) {
        return world.getWorld().getEnvironment().equals(Environment.NORMAL);
    }

    public AntiXRay(WorldServer worldServer) {
        this.enabled = false;
        for(String w: configWorlds) {
            if(w.equalsIgnoreCase(worldServer.getWorld().getName())) {
                this.enabled = true;
            }
        }
        if(this.isOverworld(worldServer)) {
            blocksToHide[7] = true;
            blocksToHide[13] = true;
            blocksToHide[14] = true;
            blocksToHide[15] = true;
            blocksToHide[16] = true;
            blocksToHide[21] = true;
            blocksToHide[56] = true;
            blocksToHide[73] = true;
            blocksToHide[129] = true;
        } else if(this.isNether(worldServer)) {
            blocksToHide[153] = true;
        }
    }

    public void issueBlockUpdates(World world, int x, int y, int z) {
        for(int i = x - 1; i <= x + 1; i++) {
            for(int j = y - 1; j <= y + 1; j++) {
                for(int k = z - 1; k <= z + 1; k++) {
                    if(j >= 0 &&  j < 256 && !(i == x && j == y && k == z)) {
                        world.notify(i, j, k);
                    }
                }
            }
        }
        int xp, yp, zp;
        for(int i = 0; i < additionalUpdatePositions.length; i++) {
            xp = additionalUpdatePositions[i][0] + x;
            yp = additionalUpdatePositions[i][1] + y;
            zp = additionalUpdatePositions[i][2] + z; 
            if(yp >= 0 && yp < 256) {
                world.notify(xp, yp, zp);
            }
        }
    }

    public void orebfuscate(byte[] buildBuffer, int dataLength, Chunk chunk, int chunkSectionsBitMask) {

        // just the lower 4 sections should be enough, thats up to height 64
        int sectionsCount = 4;
        int sectionStart = 0;
        for(int sectionID = 0; sectionID < sectionsCount; sectionID++) {
            if((chunkSectionsBitMask & (1 << sectionID)) != 0) {
                this.orebfuscateSection(sectionID, buildBuffer, dataLength, chunk, sectionStart);
                sectionStart += 4096;
            }
        }
    }

    private void orebfuscateSection(int sectionID, byte[] buildBuffer, int dataLength, Chunk chunk, int sectionStart) {
        int index = sectionStart;
        WorldServer worldServer = (WorldServer) chunk.world;
        for(int y = 0; y < 16; y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    if(index >= dataLength) {
                        //System.out.println("out of range: " + index + " > " + dataLength);
                        return;
                    }
                    int blockID = buildBuffer[index] & 255;
                    if(isOverworld(worldServer)) {
                        if(blocksToHide[blockID]) {
                            if(hasOnlySolidBlockNeighbours(chunk, sectionID, x, y, z, 1)) {
                                buildBuffer[index] = 1; // stone
                            }
                        } else if(isEnabled()) {
                            if(isOverworld(worldServer) && blockID == Block.b(Blocks.STONE)) {
                                double r = random.nextDouble();
                                if(r < 0.15D) {
                                    if(hasOnlySolidBlockNeighbours(chunk, sectionID, x, y, z, 2)) {
                                        if(r < 0.03D) {
                                            buildBuffer[index] = 56; // diamond ore
                                        } else if(r < 0.06D) {
                                            buildBuffer[index] = 15; // iron ore
                                        } else if(r < 0.09D) {
                                            buildBuffer[index] = 14; // gold ore
                                        } else if(r < 0.12D) {
                                            buildBuffer[index] = 74; // redstone or
                                        } else if(r < 0.15D) {
                                            buildBuffer[index] = 48; // mossy cobble
                                        }
                                    }
                                }
                            }
                        }
                    } else if(isNether(worldServer)) {
                        if(blocksToHide[blockID] && hasOnlySolidBlockNeighbours(chunk, sectionID, x, y, z, 1)) {
                            buildBuffer[index] = 87; // nether rack
                        }
                    }
                    index++;
                }
            }
        }
    }

    private boolean hasOnlySolidBlockNeighbours(Chunk chunk, int section, int x, int y, int z, int range) {
        boolean result = true;
        int i = range;
        try {
            while(result && i > 0) {
                result = this.checkForSolidBlocks(chunk, section, x, y, z, i);
                i--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean checkForSolidBlocks(Chunk chunk, int section, int x, int y, int z, int distance) {
        boolean allSolid = true;
        Block block;

        boolean within = (z - distance >= 0 && z + distance <= 15 && x - distance >= 0 && x + distance <= 15);
        if(!within) {
            allSolid = allSolid && this.checkBlockOfOtherPosition(chunk, section, x, y, z, distance, 0);
            allSolid = allSolid && this.checkBlockOfOtherPosition(chunk, section, x, y, z, -distance, 0);
            allSolid = allSolid && this.checkBlockOfOtherPosition(chunk, section, x, y, z, 0, distance);
            allSolid = allSolid && this.checkBlockOfOtherPosition(chunk, section, x, y, z, 0, -distance);
        } else {
            block = chunk.getTypeIdWithinSection(section, x + distance, y, z);
            allSolid = allSolid && block.r();
            block = chunk.getTypeIdWithinSection(section, x - distance, y, z);
            allSolid = allSolid && block.r();
            block = chunk.getTypeIdWithinSection(section, x, y, z + distance);
            allSolid = allSolid && block.r();
            block = chunk.getTypeIdWithinSection(section, x, y, z - distance);
            allSolid = allSolid && block.r();
        }

        if(!allSolid) { return allSolid; }

        int below = y - distance;
        int belowSection = section;
        if(below < 0) {
            below += 16;
            belowSection--;
        }
        if(belowSection >= 0) {
            block = chunk.getTypeIdWithinSection(belowSection, x, below, z);
            allSolid = allSolid && block.r();
        }

        int above = y + distance;
        int aboveSection = section;
        if(above > 15) {
            above -= 16;
            aboveSection++;
        }
        if(aboveSection < 16) {
            block = chunk.getTypeIdWithinSection(aboveSection, x, above, z);
            allSolid = allSolid && block.r();
        }
        return allSolid;
    }

    private boolean checkBlockOfOtherPosition(Chunk c, int section, int x, int y, int z, int diffX, int diffZ) {
        int newX = x + diffX;
        int newZ = z + diffZ;
        int absX = (c.locX << 4) + newX;
        int absZ = (c.locZ << 4) + newZ;
        int ox = absX >> 4;
        int oz = absZ >> 4;
        Block block;
        if(ox == c.locX && oz == c.locZ) {
            block = c.getTypeIdWithinSection(section, newX, y, newZ);
        } else {
            WorldServer worldServer = (WorldServer) c.world;
            if(worldServer.isLoaded(absX, y, absZ)) {
                block = Block.e(worldServer.getTypeId(absX, section*16 + y, absZ));
            } else {
                return false;
            }
        }
        return block.r();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
