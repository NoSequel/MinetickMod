package de.minetick.pathsearch;

import net.minecraft.server.ChunkCache;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.PathEntity;

public class PathSearchJobNavigationEntity extends PathSearchJob {

    private Entity target;

    public PathSearchJobNavigationEntity(EntityInsentient entity, Entity target, float range, boolean b1, boolean b2, boolean b3, boolean b4) {
        super(entity, range, b1, b2, b3, b4);
        this.target = target;
    }

    @Override
    public void run() {
        if(!this.issued) {
            this.issued = true;
            PathEntity pathentity = (new MinetickPathfinder(this.chunkCache, this.b1, this.b2, this.b3, this.b4)).a(entity, this.target, this.range);
            this.entity.getNavigation().setPathEntityByTarget(this.target, pathentity);
            this.cleanup();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.target = null;
    }
}
