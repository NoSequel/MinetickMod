package de.minetick;

import java.io.Serializable;
import java.util.Comparator;

import de.minetick.MinetickChunkCoordComparator.ChunkPriority;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.MathHelper;

public class MinetickChunkCoordComparator implements Comparator<ChunkCoordIntPair>, Serializable {

    private static final long serialVersionUID = 5600078159334305946L;
    private int x;
    private int z;

    private double[] normaldirectionVector;
    private boolean isXnormal;

    private double[] directionVector;
    private double[] tempMatching;

    public MinetickChunkCoordComparator (EntityPlayer entityplayer) {
        this.normaldirectionVector = new double[] { 1.0D, 0.0D };
        this.directionVector = new double[] { 1.0D, 0.0D };
        this.tempMatching = new double[] { 1.0D, 0.0D, 1.0D }; // x component, z component, squared vector length
        this.isXnormal = true;
        this.setPos(entityplayer);
    }

    public void setPos(EntityPlayer entityplayer) {
        this.x = MathHelper.floor(entityplayer.locX) >> 4;
        this.z = MathHelper.floor(entityplayer.locZ) >> 4;
        this.calcDirectionVectors(entityplayer.yaw);
        this.x -= (this.normaldirectionVector[0] * 1.5D);
        this.z -= (this.normaldirectionVector[1] * 1.5D);
    }

    @Override
    public int compare(ChunkCoordIntPair a, ChunkCoordIntPair b) {
        double weightA = this.calcChunkWeight(a);
        double weightB = this.calcChunkWeight(b);

        if(weightA < weightB) {
            return -1;
        } else if(weightA > weightB) {
            return 1;
        } else {
            return (a.x + a.z) - (b.x + b.z);
        }
    }

    private void calcDirectionVectors(float yaw) {
        this.directionVector[0] = Math.sin(Math.toRadians(yaw + 180.0f));
        this.directionVector[1] = Math.cos(Math.toRadians(yaw));
        double max;
        double absX = Math.abs(this.directionVector[0]);
        double absZ = Math.abs(this.directionVector[1]);
        if(absX > absZ) {
            this.isXnormal = true;
            max = absX;
        } else {
            this.isXnormal = false;
            max = absZ;
        }
        double factor = 1.0D / max;
        this.normaldirectionVector[0] = this.directionVector[0] * factor;
        this.normaldirectionVector[1] = this.directionVector[1] * factor;
    }

    private double[] getMatchingDirectionVector(ChunkCoordIntPair ccip) {
        int abs = Math.abs(this.isXnormal ? ccip.x - this.x : ccip.z - this.z);
        this.tempMatching[0] = this.normaldirectionVector[0] * abs;
        this.tempMatching[1] = this.normaldirectionVector[1] * abs;
        this.tempMatching[2] = (this.tempMatching[0] * this.tempMatching[0]) + (this.tempMatching[1] * this.tempMatching[1]);
        return this.tempMatching;
    }

    private double calcDistance(ChunkCoordIntPair ccip, double[] matchingVector) {
        double diffX = ccip.x - (this.x + matchingVector[0]);
        double diffZ = ccip.z - (this.z + matchingVector[1]);
        return diffX * diffX + diffZ * diffZ;
    }

    public enum ChunkPriority {
        HIGHEST(5),
        HIGH(3),
        MODERATE(2),
        LOW(1);

        private int chunksPerPacket;

        private ChunkPriority(int chunksPerPacket) {
            this.chunksPerPacket = chunksPerPacket;
        }

        public void setChunksPerPacket(int count) {
            this.chunksPerPacket = count;
        }

        public int getChunksPerPacket() {
            return this.chunksPerPacket;
        }

        public static ChunkPriority findEntry(String key) {
            try {
                ChunkPriority pri = valueOf(key);
                return pri;
            } catch (IllegalArgumentException e) {
                return null;
            } catch (NullPointerException e) {
                return null;
            }
        }
    }

    private double calcChunkWeight(ChunkCoordIntPair ccip) {
        double[] matchingVector = this.getMatchingDirectionVector(ccip);
        return (matchingVector[2] * 0.06D) + this.calcDistance(ccip, matchingVector);
    }

    public ChunkPriority getChunkPriority(ChunkCoordIntPair ccip) {
        double chunkWeight = this.calcChunkWeight(ccip);
        if(chunkWeight < 4.0D) {
            return ChunkPriority.HIGHEST;
        } else if(chunkWeight < 40.0D) {
            return ChunkPriority.HIGH;
        } else if(chunkWeight < 100.0D) {
            return ChunkPriority.MODERATE;
        } else {
            return ChunkPriority.LOW;
        }
    }
}
