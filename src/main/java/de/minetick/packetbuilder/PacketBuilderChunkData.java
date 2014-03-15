package de.minetick.packetbuilder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.Chunk;
import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;
import net.minecraft.server.TileEntity;

public class PacketBuilderChunkData {

    private Chunk chunk;
    private List<TileEntity> tileEntites;
    private boolean sendAllFlag;
    private int sectionBitmask;

    public PacketBuilderChunkData(Chunk chunk, boolean sendAllFlag, int sectionBitmask) {
        this.chunk = chunk;
        this.sendAllFlag = sendAllFlag;
        this.sectionBitmask = sectionBitmask;
        this.tileEntites = new ArrayList<TileEntity>(this.chunk.tileEntities.values());
    }

    public Chunk getChunk() {
        return this.chunk;
    }

    public boolean getSendAllFlag() {
        return this.sendAllFlag;
    }

    public int getSectionBitmask() {
        return this.sectionBitmask;
    }

    public void sendTileEntities(PlayerConnection playerconnection) {
        if(this.sectionBitmask == 0) {
            return;
        }
        for(TileEntity tileEntity: this.tileEntites) {
            int sectionId = tileEntity.z >> 4;
            if((this.sectionBitmask & (1 << sectionId)) != 0) {
                Packet packet = tileEntity.getUpdatePacket();
                if(packet != null) {
                    playerconnection.sendPacket(packet);
                }
            }
        }
    }

    public void clear() {
        this.tileEntites.clear();
        this.chunk = null;
    }
}
