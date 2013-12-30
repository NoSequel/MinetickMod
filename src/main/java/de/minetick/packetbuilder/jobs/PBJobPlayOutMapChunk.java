package de.minetick.packetbuilder.jobs;

import java.util.ArrayList;

import net.minecraft.server.Chunk;
import net.minecraft.server.Packet;
import net.minecraft.server.PacketPlayOutMapChunk;
import net.minecraft.server.PlayerConnection;
import net.minecraft.server.TileEntity;
import de.minetick.PlayerChunkSendQueue;
import de.minetick.packetbuilder.PacketBuilderBuffer;
import de.minetick.packetbuilder.PacketBuilderJobInterface;

public class PBJobPlayOutMapChunk implements PacketBuilderJobInterface {

    private PlayerConnection connection;
    private PlayerChunkSendQueue chunkQueue;
    private PlayerConnection[] connections;
    private PlayerChunkSendQueue[] chunkQueues;
    private Chunk chunk;
    private boolean flag;
    private int i;
    private boolean sendTileEntities;
    ArrayList<PlayerConnection> validOnes = new ArrayList<PlayerConnection>();
    private boolean multipleConnections;

    public PBJobPlayOutMapChunk(PlayerConnection connection, PlayerChunkSendQueue chunkQueue, Chunk chunk, boolean flag, int i) {
        this.multipleConnections = false;
        this.connection = connection;
        this.chunkQueue = chunkQueue;
        this.chunk = chunk;
        this.flag = flag;
        this.i = i;
        this.sendTileEntities = false;
    }

    public PBJobPlayOutMapChunk(PlayerConnection[] connections, PlayerChunkSendQueue[] chunkQueues, Chunk chunk, boolean flag, int i, boolean sendTileEntities) {
        this.multipleConnections = true;
        this.connections = connections;
        this.chunkQueues = chunkQueues;
        this.chunk = chunk;
        this.flag = flag;
        this.i = i;
        this.sendTileEntities = sendTileEntities;
    }

    @Override
    public void buildAndSendPacket(PacketBuilderBuffer pbb, Object checkAndSendLock) {
        boolean packetSent = false;
        PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(pbb, this.chunk, this.flag, this.i);
        if(this.multipleConnections) {
            ArrayList tileentities = null;
            // TODO: Im currently not sure if synchronizing is still required here, needs to be checked
            synchronized(checkAndSendLock) {
                if(this.sendTileEntities) {
                    tileentities = new ArrayList();
                    tileentities.addAll(chunk.tileEntities.values());
                }
                for(int a = 0; a < this.connections.length; a++) {
                    if(this.chunkQueues[a] != null && this.connections[a] != null) {
                        if(this.chunkQueues[a].isOnServer(chunk.locX, chunk.locZ)) {
                            this.validOnes.add(this.connections[a]);
                            this.connections[a] = null;
                            this.chunkQueues[a] = null;
                        }
                    }
                }
            }
            if(this.validOnes.size() > 0) {
                packet.setPendingUses(this.validOnes.size());
                packetSent = true;
                for(PlayerConnection n: this.validOnes) {
                    n.sendPacket(packet);
                    if(this.sendTileEntities) {
                        for(int i = 0; i < tileentities.size(); i++) {
                            TileEntity te = (TileEntity) (tileentities.get(i));
                            Packet p = te.getUpdatePacket();
                            if(p != null) {
                                n.sendPacket(p);
                            }
                        }
                    }
                }
            }
            this.connections = null;
            this.chunkQueues = null;
        } else {
            if(this.chunkQueue != null &&  this.connection != null) {
                synchronized(checkAndSendLock) {
                    if(!this.chunkQueue.isOnServer(chunk.locX, chunk.locZ)) {
                        packetSent = true;
                        packet.setPendingUses(1);
                        this.connection.sendPacket(packet);
                    }
                }
            }
            this.connection = null;
            this.chunkQueue = null;
        }
        if(!packetSent) {
            packet.discard();
        }
        this.clear();
    }

    public void clear() {
        this.validOnes.clear();
        this.validOnes = null;
        chunk = null;
        if(this.connections != null) {
            for(int i = 0; i < this.connections.length; i++) {
                this.connections[i] = null;
            }
            this.connections = null;
        }
        if(this.chunkQueues != null) {
            for(int i = 0; i < this.chunkQueues.length; i++) {
                this.chunkQueues[i] = null;
            }
            this.chunkQueues = null;
        }
        this.connection = null;
        this.chunkQueue = null;
    }
}
