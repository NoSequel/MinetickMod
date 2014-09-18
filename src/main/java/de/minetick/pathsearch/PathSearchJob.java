package de.minetick.pathsearch;

import net.minecraft.server.ChunkCache;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.MathHelper;
import net.minecraft.server.World;

public abstract class PathSearchJob implements Runnable {

    protected EntityInsentient entity;
    protected ChunkCache chunkCache;
    protected boolean issued;
    protected float range;
    protected boolean b1, b2, b3, b4;

    public PathSearchJob(EntityInsentient entity, float range, boolean b1, boolean b2, boolean b3, boolean b4) {
        this.entity = entity;
        this.range = range;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.b4 = b4;
        this.issued = false;
        this.createChunkCache();
    }

    private void createChunkCache() {
        int x = MathHelper.floor(this.entity.locX);
        int y = MathHelper.floor(this.entity.locY);
        int z = MathHelper.floor(this.entity.locZ);
        int radius = (int) (this.range + 8.0F);
        int xMinor = x - radius;
        int yMinor = y - radius;
        int zMinor = z - radius;
        int xMajor = x + radius;
        int yMajor = y + radius;
        int zMajor = z + radius;
        this.chunkCache = new ChunkCache(this.entity.world, xMinor, yMinor, zMinor, xMajor, yMajor, zMajor, 0);
    }

    public void cleanup() {
        this.entity = null;
        this.chunkCache = null;
    }

    @Override
    public int hashCode() {
        return this.entity.getUniqueID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PathSearchJob)) {
            return false;
        }
        return this.entity.getUniqueID().equals(((PathSearchJob)o).entity.getUniqueID());
    }
}
