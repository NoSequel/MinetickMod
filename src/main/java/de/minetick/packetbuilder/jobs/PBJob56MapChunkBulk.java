package de.minetick.packetbuilder.jobs;

import net.minecraft.server.Packet56MapChunkBulk;
import net.minecraft.server.PlayerConnection;
import de.minetick.PlayerChunkSendQueue;
import de.minetick.packetbuilder.PacketBuilderBuffer;
import de.minetick.packetbuilder.PacketBuilderChunkDataBulk;
import de.minetick.packetbuilder.PacketBuilderJobInterface;
import de.minetick.packetbuilder.PacketBuilderThreadPool;

public class PBJob56MapChunkBulk implements PacketBuilderJobInterface {

    private PlayerConnection connection;
    private PlayerChunkSendQueue chunkQueue;
    private PacketBuilderChunkDataBulk chunkData;

    private PacketBuilderBuffer pbb;

    public PBJob56MapChunkBulk(PlayerConnection connection, PacketBuilderChunkDataBulk chunkData, PlayerChunkSendQueue chunkQueue) {
        this.connection = connection;
        this.chunkData = chunkData;
        this.chunkQueue = chunkQueue;
    }

    @Override
    public void run() {
        if(this.chunkQueue == null || this.connection == null) {
            this.chunkData.clear();
            this.clear();
            return;
        }
        Packet56MapChunkBulk packet = new Packet56MapChunkBulk(this.pbb, this.chunkData.getChunks());
        boolean allStillListed = this.chunkData.verifyLoadedChunks(this.chunkQueue);
        if(allStillListed) {
            packet.setPendingUses(1);
            this.connection.sendPacket(packet);
            this.chunkData.sendTileEntities(this.connection);
            this.chunkData.queueChunksForTracking(this.connection.player, this.chunkQueue);
            this.chunkData.clear();
        } else {
            packet.discard();
        }
        if(!allStillListed && !this.chunkData.isEmpty()) {
            PacketBuilderThreadPool.addJobStatic(new PBJob56MapChunkBulk(this.connection, this.chunkData, this.chunkQueue));
        }
        this.clear();
    }

    public void clear() {
        this.chunkData = null;
        this.connection = null;
        this.chunkQueue = null;
        this.pbb = null;
    }

    @Override
    public void assignBuildBuffer(PacketBuilderBuffer pbb) {
        this.pbb = pbb;
    }
}
