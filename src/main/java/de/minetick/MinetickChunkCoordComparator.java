package de.minetick;

import java.io.Serializable;
import java.util.Comparator;

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
        double[] matchingVectorA = this.getMatchingDirectionVector(a);
        double distanceA = (matchingVectorA[2] * 0.06D) + this.calcDistance(a, matchingVectorA);

        double[] matchingVectorB = this.getMatchingDirectionVector(b);
        double distanceB = (matchingVectorB[2] * 0.06D) + this.calcDistance(b, matchingVectorB);
        if(distanceA < distanceB) {
            return -1;
        } else if(distanceA > distanceB) {
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
}
