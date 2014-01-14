package de.minetick.packetbuilder.jobs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.Chunk;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet;
import net.minecraft.server.PacketPlayOutMapChunkBulk;
import net.minecraft.server.PlayerConnection;
import net.minecraft.server.TileEntity;
import de.minetick.PlayerChunkSendQueue;
import de.minetick.packetbuilder.PacketBuilderBuffer;
import de.minetick.packetbuilder.PacketBuilderJobInterface;
import de.minetick.packetbuilder.PacketBuilderThreadPool;

public class PBJobPlayOutMapChunkBulk implements PacketBuilderJobInterface {

    private PlayerConnection connection;
    private List<WeakReference<Chunk>> chunks;
    private PlayerChunkSendQueue chunkQueue;

    public PBJobPlayOutMapChunkBulk(PlayerConnection connection, List<WeakReference<Chunk>> chunks, PlayerChunkSendQueue chunkQueue) {
        this.connection = connection;
        this.chunks = chunks;
        this.chunkQueue = chunkQueue;
    }

    @Override
    public void buildAndSendPacket(PacketBuilderBuffer pbb, Object checkAndSendLock) {
        PacketPlayOutMapChunkBulk packet = new PacketPlayOutMapChunkBulk(pbb, this.chunks);
        boolean allStillListed = true;
        // TODO: Im currently not sure if synchronizing is still required here, needs to be checked
        synchronized(checkAndSendLock) {
            Iterator<WeakReference<Chunk>> iter = this.chunks.iterator();
            while(iter.hasNext()) {
                WeakReference<Chunk> c = iter.next();
                if(this.chunkQueue != null && this.connection != null) {
                    if(!this.chunkQueue.isOnServer(c.get().locX, c.get().locZ)) {
                        allStillListed = false;
                        iter.remove();
                    }
                }
            }
            if(allStillListed) {
                packet.setPendingUses(1);
                this.connection.sendPacket(packet);
                ArrayList arraylist1 = new ArrayList();
                for(WeakReference<Chunk> c : this.chunks) {
                    arraylist1.addAll(c.get().tileEntities.values());
                }
                Iterator iterator2 = arraylist1.iterator();
                EntityPlayer entityplayer = this.connection.player;
                while (iterator2.hasNext()) {
                    TileEntity tileentity = (TileEntity) iterator2.next();
                    if (tileentity != null) {
                        Packet p = tileentity.getUpdatePacket();
                        if(p != null) {
                            this.connection.sendPacket(p);
                        }
                    }
                }
                for(WeakReference<Chunk> wr: this.chunks) {
                    entityplayer.chunksForTracking.add(wr.get());
                }
                this.chunks.clear();
            } else {
                packet.discard();
            }
        }
        if(!allStillListed && !this.chunks.isEmpty()) {
            PacketBuilderThreadPool.addJobStatic(new PBJobPlayOutMapChunkBulk(this.connection, this.chunks, this.chunkQueue));
        }
        this.clear();
    }

    public void clear() {
        this.chunks = null;
        this.connection = null;
        this.chunkQueue = null;
    }
}
