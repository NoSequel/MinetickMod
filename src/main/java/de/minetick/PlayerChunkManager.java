package de.minetick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import de.minetick.MinetickChunkCoordComparator.ChunkPriority;
import de.minetick.packetbuilder.PacketBuilderChunkDataBulk;
import de.minetick.packetbuilder.PacketBuilderThreadPool;
import de.minetick.packetbuilder.jobs.PBJob56MapChunkBulk;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.NetworkManager.SendQueueFillLevel;
import net.minecraft.server.Packet56MapChunkBulk;
import net.minecraft.server.PlayerChunk;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.PlayerChunkMap;
import net.minecraft.server.TileEntity;
import net.minecraft.server.WorldData;
import net.minecraft.server.WorldServer;
import net.minecraft.server.WorldType;

public class PlayerChunkManager {

    private List<EntityPlayer> shuffleList = Collections.synchronizedList(new LinkedList<EntityPlayer>());
    private boolean skipHeavyCalculations = false;
    private int chunkCreated = 0;
    private WorldServer world;
    private PlayerChunkMap pcm;
    public static int packetsPerTick = 1;

    private Map<String, PlayerChunkBuffer> playerBuff = new HashMap<String, PlayerChunkBuffer>();

    public PlayerChunkManager(WorldServer world, PlayerChunkMap pcm) {
        this.world = world;
        this.pcm = pcm;
    }

    public PlayerChunkMap getPlayerChunkMap() {
        return this.pcm;
    }

    private String getMapKey(EntityPlayer entity) {
        return entity.getBukkitEntity().getName();
    }

    public PlayerChunkBuffer getChunkBuffer(EntityPlayer entityplayer) {
        return this.playerBuff.get(this.getMapKey(entityplayer));
    }

    public PlayerChunkBuffer addPlayer(EntityPlayer entityplayer) {
        String mapkey = this.getMapKey(entityplayer);
        PlayerChunkBuffer buff = this.playerBuff.get(mapkey);
        if(buff == null) {
            buff = new PlayerChunkBuffer(this, entityplayer);
            synchronized(this.playerBuff) {
                this.playerBuff.put(mapkey, buff);
            }
        } else {
            buff.clear();
        }
        this.shuffleList.add(entityplayer);
        return buff;
    }

    public void removePlayer(EntityPlayer entityplayer) {
        this.shuffleList.remove(entityplayer);
        String mapkey = this.getMapKey(entityplayer);
        PlayerChunkBuffer buff = this.playerBuff.get(mapkey);
        if(buff != null) {
            buff.clear();
        }
        synchronized(this.playerBuff) {
            this.playerBuff.remove(mapkey);
        }
    }

    public boolean skipChunkGeneration() {
        return this.skipHeavyCalculations;
    }

    public void skipChunkGeneration(boolean skip) {
        this.skipHeavyCalculations = skip;
    }

    public boolean alreadyEnqueued(EntityPlayer entityplayer, ChunkCoordIntPair ccip) {
        PlayerChunkBuffer buff = this.playerBuff.get(this.getMapKey(entityplayer));
        if(buff == null) {
            return false;
        }
        if(buff.contains(ccip)) {
            return true;
        }
        return false;
    }

    public int updatePlayers(ChunkGenerationPolicy chunkGenerationPolicy) {
        int allGenerated = 0;
        EntityPlayer[] array = this.shuffleList.toArray(new EntityPlayer[0]);
        for(int i = 0; i < array.length; i++) {
            EntityPlayer entityplayer = array[i];
            PlayerChunkBuffer buff = this.playerBuff.get(this.getMapKey(entityplayer));
            if(buff == null) {
                continue;
            }
            buff.resetCounters();
            buff.updatePos(entityplayer);
            int playerChunkPosX = buff.getPlayerRegionCenter()[0];
            int playerChunkPosZ = buff.getPlayerRegionCenter()[1];

            // High priority chunks
            PriorityQueue<ChunkCoordIntPair> queue = buff.getHighPriorityQueue();
            while(queue.size() > 0 && buff.loadedChunks < (this.skipHeavyCalculations ? 5: 15)) {
                ChunkCoordIntPair ccip = queue.poll();
                if(buff.getPlayerChunkSendQueue().isOnServer(ccip) && !buff.getPlayerChunkSendQueue().alreadyLoaded(ccip)) {
                    PlayerChunk c = this.pcm.a(ccip.x, ccip.z, true);
                    c.a(entityplayer);
                    if(buff.getPlayerChunkSendQueue().queueForSend(c, entityplayer)) {
                        buff.loadedChunks++;
                    }
                }
                buff.remove(ccip);
            }

            // Low priority chunks
            queue = buff.getLowPriorityQueue();
            while(queue.size() > 0 && buff.loadedChunks < 5 && !this.skipHeavyCalculations) {
                ChunkCoordIntPair ccip = queue.poll();
                if(buff.getPlayerChunkSendQueue().isOnServer(ccip) && !buff.getPlayerChunkSendQueue().alreadyLoaded(ccip)) {
                    boolean chunkExists = this.world.chunkProviderServer.doesChunkExist(ccip.x, ccip.z);
                    if(!this.skipHeavyCalculations && (chunkExists || chunkGenerationPolicy.isChunkGenerationCurrentlyAllowed(this.world.getWorld().getWorldType()))) {
                        PlayerChunk c = this.pcm.a(ccip.x, ccip.z, true);
                        c.a(entityplayer);
                        if(c.isNewChunk()) {
                            buff.generatedChunks++;
                            allGenerated++;
                            chunkGenerationPolicy.generatedChunk();
                        } else {
                            buff.loadedChunks++;
                        }
                        buff.getPlayerChunkSendQueue().queueForSend(c, entityplayer);
                        buff.remove(ccip);
                    } else {
                        buff.skippedChunks++;
                    }
                } else {
                    buff.remove(ccip);
                }
            }
            PlayerChunkSendQueue chunkQueue = buff.getPlayerChunkSendQueue();
            int previouslyskipped = chunkQueue.requeuePreviouslySkipped();
            if(buff.generatedChunks > 0 || buff.loadedChunks > 0 || buff.enlistedChunks > 0 || previouslyskipped > 0) {
                chunkQueue.sort(entityplayer);
            }
            if(buff.generatedChunks > 0) {
                this.shuffleList.remove(entityplayer);
                this.shuffleList.add(entityplayer);
            }

            int skipped = 0;
            int packetCount = packetsPerTick;
            SendQueueFillLevel level = entityplayer.playerConnection.getSendQueueFillLevel();
            if(level.isEqualOrHigherThan(SendQueueFillLevel.MEDIUM)) {
                packetCount = 0;
            }
            for(int w = 0; w < packetCount; w++) {
                PacketBuilderChunkDataBulk chunkData = new PacketBuilderChunkDataBulk();
                while(chunkQueue.hasChunksQueued() && chunkData.size() < 5 && skipped < (20 + previouslyskipped)) {
                    ChunkCoordIntPair chunkcoordintpair = chunkQueue.peekFirst();
                    if (chunkcoordintpair != null && chunkQueue.isOnServer(chunkcoordintpair)) {
                        if(this.world.isLoaded(chunkcoordintpair.x << 4, 0, chunkcoordintpair.z << 4)) {
                            Chunk chunk = this.world.getChunkAt(chunkcoordintpair.x, chunkcoordintpair.z);
                            //if(chunk.k()) {
                                chunkData.addChunk(chunk);
                                chunkQueue.removeFirst(true);
                                ChunkPriority priority = buff.comp.getChunkPriority(chunkcoordintpair);
                                if(chunkData.size() >= priority.getChunksPerPacket()) {
                                    break;
                                }
                            /*
                            } else {
                                chunkQueue.skipFirst();
                                skipped++;
                            }
                            */
                        } else {
                            chunkQueue.skipFirst();
                            skipped++;
                        }
                    } else {
                        chunkQueue.removeFirst(false);
                    }
                }
                if (!chunkData.isEmpty()) {
                    PacketBuilderThreadPool.addJobStatic(new PBJob56MapChunkBulk(entityplayer.playerConnection, chunkData, chunkQueue));
                }
            }
        }
        return allGenerated;
    }

    public static boolean isWithinRadius(int positionx, int positionz, int centerx, int centerz, int radius) {
        int distancex = positionx - centerx;
        int distancez = positionz - centerz;

        return (distancex >= -radius && distancex <= radius) ? (distancez >= -radius && distancez <= radius) : false;
    }

    public boolean doAllCornersOfPlayerAreaExist(int x, int z, int radius) {
        boolean exists = this.world.chunkExists(x - radius, z - radius);
        exists = exists && this.world.chunkExists(x + radius, z + radius);
        if(!exists) { return false; }
        exists = exists && this.world.chunkExists(x - radius, z + radius);
        return exists && this.world.chunkExists(x + radius, z - radius);
    }

    public static int[] get2DDirectionVector(EntityPlayer entityplayer) {
        double rotation = entityplayer.yaw;
        if (157.5 <= rotation && rotation < -157.5) {
            return new int[]{0,-1}; // north
        } else if (-157.5 <= rotation && rotation < -112.5) {
            return new int[]{1,-1};
        } else if (-112.5 <= rotation && rotation < -67.5) {
            return new int[]{1, 0};
        } else if (-67.5 <= rotation && rotation < -22.5) {
            return new int[]{1, 1};
        } else if (-22.5 <= rotation && rotation < 22.5) {
            return new int[]{0, 1};
        } else if (22.5 <= rotation && rotation < 67.5) {
            return new int[]{-1, 1};
        } else if (67.5 <= rotation && rotation < 112.5) {
            return new int[]{-1,0};
        } else if (112.5 <= rotation && rotation < 157.5) {
            return new int[]{-1,-1};
        } else {
            return new int[]{0,0};
        }
    }
}
